(defproject robert/hooke "1.1.2"
  :description "Hooke your functions!"
  :dev-dependencies [[org.clojure/clojure "1.3.0-beta1"]
                     #_[lein-multi "1.0.0"]]
  ;; TODO: lein-multi is not working here
  :multi-deps {"1.2" [[org.clojure/clojure "1.2.1"]]}
  :test-selectors {:default (complement :skip)})
