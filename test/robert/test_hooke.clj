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

(deftest test-clear-hooks
  (is (nil? (meta @#'hooked)))
  (add-hook #'hooked #'asplode)
  (is (not (nil? (meta @#'hooked))))
  (clear-hooks #'hooked)
  (is (nil? (meta @#'hooked)))
  (is (= nil (clear-hooks #'hooked)))
  (is (nil? (meta @#'hooked))))

(defn print-name [name]
  (println name))

(deftest test-prepend
  (prepend print-name
           (println "The following person is awesome:"))
  (is (= "The following person is awesome:\nGilbert K. Chesterton\n"
         (with-out-str
           (print-name "Gilbert K. Chesterton")))))

(defn ohai []
  :hello)

(def appended (atom false))

(deftest test-append
  (append ohai (reset! appended true))
  (is (= :hello (ohai)))
  (is @appended))

(defn ^{:dynamic true} another-fn []
  true)

(deftest test-without-hooks
  (add-hook #'another-fn asplode)
  (is (thrown? Exception (another-fn)))
  (with-hooks-disabled another-fn
    (is (another-fn))))

(def skip-ran? (atom false))

(deftest ^{:skip true} skipped
  (reset! skip-ran? true))

(deftest hooks-disabled-works-around-test-selectors
  (with-hooks-disabled test-var
    (skipped)))
