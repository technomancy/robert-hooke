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
  Use the add-hook function to wrap a function in your a hook."
  (:require [flatland.ordered.map :as om]))

(defn- hooks [v]
  (-> @v meta ::hooks))

(defn- original [v]
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
  (when-not (hooks v)
    (let [hooks (atom (om/ordered-map))]
      (alter-var-root v (fn [original]
                          (with-meta
                            (fn [& args]
                              (run-hooks (vals @hooks) original args))
                            (assoc (meta original)
                                   ::hooks hooks
                                   ::original original)))))))

(defonce hook-scopes [])

(defn start-scope []
  (locking hook-scopes
    (alter-var-root #'hook-scopes conj {})))

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
