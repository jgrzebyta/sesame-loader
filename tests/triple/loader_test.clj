(ns triple.loader-test
  (:use [triple.loader]
        [triple.repository]
        [triple.reifiers :only [chunk-commiter]]
        [clojure.test]
        [clojure.tools.logging :as log]
        [clojure.java.io :as jio])
  (:import [org.eclipse.rdf4j.common.iteration CloseableIteration]
           [org.eclipse.rdf4j.model Resource Statement]
           [org.eclipse.rdf4j.rio Rio RDFFormat ParserConfig RDFParseException]
           [org.eclipse.rdf4j.repository RepositoryResult RepositoryConnection]
           [org.eclipse.rdf4j.repository.http HTTPRepository]
           [org.eclipse.rdf4j.repository.sail SailRepository]
           [org.eclipse.rdf4j.sail.memory MemoryStore]))

(defn count-statements "Counts statements in SPARQL result." [^CloseableIteration result]
  (try
    (loop [count 0]
      (if (.hasNext result)
        (do
          (.next result)  ;; ignore result
          (recur (inc count)))
        count))
    (catch Exception e (log/error "Some error: " (.getMessage e)))
    (finally (.close result))))


#_(deftest connect-triple 							; temporary switching off integration test
    (testing "Test initialising connection."
      (let [server-url "http://localhost:8080/openrdf-sesame"
            repository-id "test"
            init-connection-f #'triple.loader/init-connection]                  ; access to prive function
        (with-open-repository (c (HTTPRepository. server-url repository-id))
          (init-connection-f c)
          (is (instance? org.eclipse.rdf4j.repository.RepositoryConnection c))
          (log/debug "Repository connection class: %s" (class c))
          (is (.isOpen c))))))

(deftest open-file
  (with-open [fr (jio/reader "tests/beet.rdf")]
    (println "reader?" (class fr))
    (testing "Is Reader instantiated"
      (is (instance? java.io.BufferedReader fr)))
    (testing "Reads any character"
      (let [lines (count (line-seq fr))]
        (is (= 175 lines))
        (println (format "File contains %d lines" lines)))
      )))


(defn test-repository "Does more detailed tests on storage" [repository expected]
  (is (instance? SailRepository repository))
  (log/debug "repository class: " (class repository))
  (with-open [c (.getConnection repository)]
    (let [result (.getStatements c nil nil nil false (into-array Resource '[]))
          statement-total (count-statements result)]
      (is (= expected statement-total))
      (log/debug (format "Found %d statements" statement-total))
      )))

(deftest load-mock-repo
  (let [repo (make-mem-repository)
        pars (Rio/createParser RDFFormat/RDFXML)
        file-obj (jio/file "tests/beet.rdf")]
    (testing "Loading data to repository"
      (with-open [conn (.getConnection repo)
                  fr (jio/reader file-obj)]
        ;; parse file
        (log/debug "start file parsing")
        (.setRDFHandler pars (chunk-commiter conn))
        (.parse pars fr (.toString (.toURI file-obj)))
        (.commit conn)
        (is (not (.isEmpty conn)))
        (log/debug "Is Connection empty?: " (.isEmpty conn)))
      )
    (testing "Does more tests ... "
      (test-repository repo 68)))
  )
