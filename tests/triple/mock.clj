(ns triple.mock
  (:use [clojure.java.io :as jio]
        [clojure.test :as test])
  (:import [org.openrdf.rio Rio RDFFormat RDFParserFactory]
           [org.openrdf.rio.turtle TurtleParserFactory]
           [org.openrdf.rio.helpers StatementCollector]))


(defn read-rdf-file "Reads RDF file"
  [filename filetype]
  (with-open [rdr (jio/input-stream filename)]
    (let [format (case filetype
                 "n3" RDFFormat/N3
                 "nq" RDFFormat/NQUADS
                 "rdfxml" RDFFormat/RDFXML
                 "rdfa" RDFFormat/RDFA
                 "turtle" RDFFormat/TURTLE
                 RDFFormat/TURTLE)
          rdf-parser (Rio/createParser format)
          collector (StatementCollector.)]
      (.setRDFHandler rdf-parser collector)
      (println (format "Parser: %s" (type rdf-parser)))
      ;; run parsing process
      (.parse rdf-parser rdr "urn:data")
      collector)))

(deftest test-reading "Provides main reading job" []
  (let [collector (read-rdf-file "tests/22-rdf-syntax-ns.ttl" "turtle")]
    (testing "Test RDF file reading"
;;      (is (instance? collector))
      (is (< 0 (count (.getNamespaces collector))))
      (println (format "namespaces: %s" (.getNamespaces collector))))))
