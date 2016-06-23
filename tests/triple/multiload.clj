(ns triple.multiload
  (:use [sparql :exclude [-main]]
        [triple.repository]
        [clojure.test]
        [clojure.tools.logging :as log]
        [clojure.java.io :as jio]))


(def +datasets+ '({:data-file "./tests/resources/22-rdf-syntax-ns.ttl" :type "turtle"}
                 {:data-file "./tests/resources/rdf-schema.rdf" :type "rdfxml"}))


(deftest test-load-multidata "load multiple data."
  (let [repo (make-repository-with-lucene)]
    (sparql/load-multidata repo +datasets+ 3)
    ;; (testing "count repository content"
    ;;   (let [sts (get-statements repo)]
    ;;     (log/info "Fount statements: " (count sts))
    ;;     (is (> 0 (count sts)))
    ;;     )
    ;;   )
    ))
