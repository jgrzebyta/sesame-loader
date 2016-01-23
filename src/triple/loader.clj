;; file name must follow namespace's name

(ns triple.loader
  (:gen-class)
  (:require [clojure.tools.cli :refer [cli]]
            [clojure.java.io :as jio]
            [triple.reifiers :as ref])
  (:import [org.openrdf.repository.http HTTPRepository HTTPRepositoryConnection]
           [org.openrdf.repository RepositoryConnection]
           [org.openrdf.rio Rio RDFFormat ParserConfig]
           [org.openrdf.rio.helpers BasicParserSettings]
           [org.openrdf.query QueryLanguage]
           [org.apache.commons.logging LogFactory]))





(defn make-parser-config []
    (doto
        (ParserConfig.)
        (.set BasicParserSettings/PRESERVE_BNODE_IDS true)))


(defn init-connection "Initialise HTTPRepository class to sesame remote repository."
  [server-url repository-id]
  (let [repo (HTTPRepository. server-url repository-id)]
    (.initialize repo)
    (doto  ;; create connection 
        (.getConnection repo)
      (.setParserConfig (make-parser-config))
      (.setAutoCommit false))))


(defn do-loading [opts]
  (with-open [c (init-connection (:s opts) (:r opts))
              reader-file (jio/reader (:f opts))]
    (let [format (case (:t opts)
                 "n3" RDFFormat/N3
                 "nq" RDFFormat/NQUADS
                 "rdfxml" RDFFormat/RDFXML
                 "rdfa" RDFFormat/RDFA
                 RDFFormat/TURTLE)
          parser (doto
                     (Rio/createParser format)
                   (.setRDFHandler (ref/chunk-commiter c)))]
      (.parse parser reader-file nil))))


(defn -main [& args]
  (let [[opts args banner] (cli args
                               ["--help" "-h" "Print this screen" :default false :flag true]
                               ["--server URL" "-s" "Sesame SPARQL endpoint URL" :default "http://localhost:8080/openrdf-sesame"]
                               ["--repositiry NAME" "-r" "Repository id" :default "test"]
                               ["--file FILE" "-f" "Data file path"]
                               ["--file-type TYPE" "-t" "Data file type. One of: n3, nq, rdfxml, rdfa" :default "turtle"])]
  ;; print help message
  (when (:h opts)
    (println banner)
    (System/exit 0))

  ;; run proper loading
  (do-loading opts)))


