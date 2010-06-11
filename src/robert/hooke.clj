(ns robert.hooke
  "Hooke your functions!

    (defn examine [x]
      (println x))

    (defn microscope
      \"The keen powers of observation enabled by Robert Hooke allow
      for a closer look at any object!\"
      [f x]
      (f (.toUpperCase x)))

    (defn doubler [f & args]
      (apply f args)
      (apply f args))

    (defn telescope [f x]
      (f (apply str (interpose \" \" x))))

    (add-hook #'examine microscope)
    (add-hook #'examine doubler)
    (add-hook #'examine telescope)

    ;; Now when we examine something:
    (examine \"something\")
    > SOMETHING
    > SOMETHING

  Use the add-hook function to wrap a function in your a hook.")

(defn- compose-hooks [f1 f2]
  (fn [& args]
    (apply f2 f1 args)))

(defn- join-hooks [original hooks]
  (reduce compose-hooks original hooks))

(defn- run-hooks [hooked original args]
  (apply (join-hooks original @(::hooks (meta hooked))) args))

(defn- prepare-for-hooks [v]
  (when-not (::hooks (meta @v))
    (alter-var-root v (fn [original]
                        (with-meta
                          (fn runner [& args]
                            (run-hooks runner original args))
                          (assoc (meta original)
                            ::hooks (atom ()) ::original original))))))

(defn add-hook
  "Add a hook function f to target-var. Hook functions are passed the
  target function and all their arguments and must apply the target to
  the args if they wish to continue execution."
  [target-var f]
  (prepare-for-hooks target-var)
  (swap! (::hooks (meta @target-var)) conj f))

(defn remove-hook
  "Remove hook function f from target-var."
  [target-var f]
  (when (::hooks (meta @target-var))
    (swap! (::hooks (meta @target-var))
           (partial remove #{f}))
    (when (empty? @(::hooks (meta @target-var)))
      (alter-var-root target-var
                      (constantly (::original (meta @target-var)))))))
