(ns robert.test-hooke
  (:use [robert.hooke] :reload-all)
  (:use [clojure.test]))

(deftest test-no-arg
  (defn no-args []
    (println "mine"))
  (add-hook #'no-args (fn h1 [f] (do (println "h1") (f))))
  (add-hook #'no-args (fn h2 [f & args] (do (println "h2") (apply f args))))
  (is (= "h1\nh2\nmine\n"
         (with-out-str (no-args)))))

(deftest test-one-arg
  (defn one-arg [x]
    (println "arg" x))
  (add-hook #'one-arg (fn h2 [f & args] (do (println "h2") (apply f args))))
  (is (= "h2\narg hi\n"
         (with-out-str (one-arg "hi")))))

(defn hooked []
  true)
(defn asplode [f]
  (/ 9 0))

(deftest test-remove-hook
  (let [orig-hooked hooked]
    (add-hook #'hooked asplode)
    (is (not= hooked orig-hooked))
    (is (thrown? Exception (hooked)))
    (remove-hook #'hooked asplode)
    (is (= hooked orig-hooked))
    (is (hooked))))
