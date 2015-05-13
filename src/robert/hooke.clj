(ns robert.hooke
  "Hooke your functions!
    (defn examine [x]
      (println x))
    (defn microscope
      \"The keen powers of observation enabled by Robert Hooke allow
      for a closer look at any object!\"
      [f x]
      (f (.toUpperCase (str x))))
    (defn doubler [f & args]
      (apply f args)
      (apply f args))
    (defn telescope [f x]
      (f (apply str (interpose \" \" x))))
    (add-hook #'examine #'microscope)
    (add-hook #'examine #'doubler)
    (add-hook #'examine #'telescope)
    ;; Now when we examine something:
    (examine \"something\")
    > S O M E T H I N G
    > S O M E T H I N G
  Use the add-hook function to wrap a function in your a hook.")

(defn- hooks [v]
  "Deref a var then get ::hooks in meta"
  (-> @v meta ::hooks))

(defn- original [v]
  "Deref a var then get ::original in meta"
  (-> @v meta ::original))

(defn- compose-hooks [f1 f2]
  (fn [& args]
    ;; TODO: tracing
    (apply f2 f1 args)))

(defn- join-hooks [original hooks]
  (reduce compose-hooks original hooks))

(defn- run-hooks [hooks original args]
  (apply (join-hooks original hooks) args))

(defn- prepare-for-hooks [v]
  "Add an empty hooks map and the original function to the metadata.
  Make the var of the original function point to a new function with all hooks
  joined.
  Array map is used to ensure joining order of hooks.

  Examples:
  (defn examine [x]
    (println x))
  (defn h1 [f x]
    (f (+ 1 x)))
  (defn h2 [f x]
    (f (* x 2)))
  (defn h3 [f x]
    (f (- x 3)))
  (defn h4 [f x]
    (f (* x 4)))
  (add-hook #'examine #'h3)
  (add-hook #'examine #'h4)
  (add-hook #'examine #'h1)
  (add-hook #'examine #'h2)
  (examine 7)

  In the above example the joining order of hooks is:
  h3, h4, h1, h2, as the user has specified.
  "
  (when-not (hooks v)
    (let [hooks (atom (array-map))]
      (alter-var-root v (fn [original]
                          (with-meta
                            (fn [& args]
                              (run-hooks (vals @hooks) original args))
                            (assoc (meta original)
                              ::hooks hooks
                              ::original original)))))))

(defonce hook-scopes [])

(defn- scope-update-fn
  [scopes target-var]
  (conj
    (pop scopes)
    (update-in (peek scopes) [target-var] #(if % % @(hooks target-var)))))

(defn- possibly-record-in-scope
  [target-var]
  (locking hook-scopes
    (when (seq hook-scopes)
      (alter-var-root #'hook-scopes scope-update-fn target-var))))

(defn add-hook
  "Add a hook function f to target-var. Hook functions are passed the
  target function and all their arguments and must apply the target to
  the args if they wish to continue execution."
  ([target-var f]
   (add-hook target-var f f))
  ([target-var key f]
   (prepare-for-hooks target-var)
   (possibly-record-in-scope target-var)
   (swap! (hooks target-var) assoc key f)))


(defn- clear-hook-mechanism [target-var]
  (alter-var-root target-var
                  (constantly (original target-var))))

(defn remove-hook
  "Remove hook identified by key from target-var."
  [target-var key]
  (when-let [hooks (hooks target-var)]
    (swap! hooks dissoc key)
    (when (empty? @hooks)
      (clear-hook-mechanism target-var))))

(defn clear-hooks
  "Remove all hooks from target-var."
  [target-var]
  (when-let [hooks (hooks target-var)]
    (swap! hooks empty)
    (clear-hook-mechanism target-var)))

(defmacro prepend [target-var & body]
  `(add-hook (var ~target-var) (fn [f# & args#]
                                 ~@body
                                 (apply f# args#))))

(defmacro append [target-var & body]
  `(add-hook (var ~target-var) (fn [f# & args#]
                                 (let [val# (apply f# args#)]
                                   ~@body
                                   val#))))

(defmacro with-hooks-disabled [f & body]
  `(do (when-not (#'hooks (var ~f))
         (throw (Exception. (str "No hooks on " ~f))))
       (with-redefs [~f (#'original (var ~f))]
         ~@body)))


(defn start-scope []
  (locking hook-scopes
    (alter-var-root #'hook-scopes conj {})))


(defn end-scope []
  (locking hook-scopes
    (let [head (peek hook-scopes)]
      (alter-var-root #'hook-scopes pop)
      (doseq [[var old-hooks] head]
        (reset! (hooks var) old-hooks)))))

(defmacro with-scope
  "Defines a scope which records any change to hooks during the dynamic extent
of its body, and restores hooks to their original state on exit of the scope."
  [& body]
  `(try
     (start-scope)
     ~@body
     (finally (end-scope))))

