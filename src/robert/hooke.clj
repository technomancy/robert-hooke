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

(defn- compose-hooks [f1 f2]
  (fn [& args]
    (apply f2 f1 args)))

(defn- join-hooks [original hooks]
  (reduce compose-hooks original hooks))

(defn- run-hooks [hook original args]
  (apply (join-hooks original (vals @hook)) args))

(defn- prepare-for-hooks [v]
  (when-not (:robert.hooke/hook (meta @v))
    (let [hook (atom {})]
      (alter-var-root v (fn [original]
                          (with-meta
                            (fn [& args]
                              (run-hooks hook original args))
                            (assoc (meta original)
                              :robert.hooke/hook hook
                              :robert.hooke/original original))))))) 

(defn add-hook
  "Add a hook function f to target-var. Hook functions are passed the
  target function and all their arguments and must apply the target to
  the args if they wish to continue execution."
  ([target-var f]
     (add-hook target-var f f))
  ([target-var key f]
     (prepare-for-hooks target-var)
     (swap! (:robert.hooke/hook (meta @target-var)) assoc key f)))

(defn- clear-hook-mechanism [target-var]
  (alter-var-root target-var
                  (constantly (:robert.hooke/original
                               (meta @target-var)))))

(defn remove-hook
  "Remove hook identified by key from target-var."
  [target-var key]
  (when (:robert.hooke/hook (meta @target-var))
    (swap! (:robert.hooke/hook (meta @target-var))
           dissoc key)
    (when (empty? @(:robert.hooke/hook (meta @target-var)))
      (clear-hook-mechanism target-var))))

(defn clear-hooks
  "Remove all hooks from target-var."
  [target-var]
  (when (:robert.hooke/hook (meta @target-var))
    (swap! (:robert.hooke/hook (meta @target-var)) empty)
    (when (empty? @(:robert.hooke/hook (meta @target-var)))
      (clear-hook-mechanism target-var))))

(defmacro prepend [target-var & body]
  `(add-hook (var ~target-var) (fn [f# & args#]
                                 ~@body
                                 (apply f# args#))))

(defmacro append [target-var & body]
  `(add-hook (var ~target-var) (fn [f# & args#]
                                 (let [val# (apply f# args#)]
                                   ~@body
                                   val#))))

(defmacro with-hooks-disabled [v & body]
  `(do (when-not (:robert.hooke/hook (meta ~v))
         (throw (Exception. (str "No hooks on " ~v))))
       (with-redefs [~v (:robert.hooke/original (meta ~v))]
         ~@body)))
