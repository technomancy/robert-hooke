(defproject robert/hooke "1.1.0"
  :description "Hooke your functions!"
  :dev-dependencies [[org.clojure/clojure "1.2.0"]
                     [swank-clojure "1.2.1"]]
  :test-selectors {:default (complement :skip)})
