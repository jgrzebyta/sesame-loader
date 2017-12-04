(ns rdf4j.collection-test
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.test :as t]
            [clojure.tools.logging :as log]
            [rdf4j.collections :as c]
            [rdf4j.collections.utils :as cu]
            [rdf4j.loader :as l]
            [rdf4j.models :as m]
            [rdf4j.protocols :as p]
            [rdf4j.repository :as r]
            [rdf4j.triples-source.wrappers :as w]
            [rdf4j.utils :as u])
  (:import [org.eclipse.rdf4j.model Model BNode]
           [org.eclipse.rdf4j.model.util Models]
           [org.eclipse.rdf4j.model.vocabulary RDF RDFS XMLSchema]
           [org.eclipse.rdf4j.rio RDFFormat]
           [rdf4j.protocols ModelHolder]))

(def ^{:static true} vf (u/value-factory))

(t/deftest loaded-model
  (t/testing "load data into model."
    (let [file-obj (io/file "tests/resources/collections/multisubj.ttl")
          repository (r/make-repository)]
      (l/load-data repository file-obj)
      (let [^Model model (m/loaded-model (r/get-all-statements repository))]
        (t/is (not (.isEmpty model)))
        (log/debugf "Number of statements: %d" (.size model))
        (t/is (not (m/single-subjectp model)))))))


(t/deftest single-subject-test
  (t/testing "Test single-subjectp predicate"
    (let [file-obj (io/file "tests/resources/collections/multisubj2.ttl" )
          repository (r/make-repository)]
      (l/load-data repository file-obj)
      (let [^Model model (m/loaded-model (r/get-all-statements repository))]
        (t/is (not (m/single-subjectp model)))))
    
    (let [file-obj (io/file "tests/resources/collections/multisubj3.ttl" )
          repository (r/make-repository)]
      (l/load-data repository file-obj)
      (let [^Model model (m/loaded-model (r/get-all-statements repository))]
        (t/is (m/single-subjectp model))))))

(t/deftest loaded-model-test
  (t/testing "Test Model loading"
    (let [file-obj (io/file "tests/resources/collections/type-list.ttl" )
          model (m/loaded-model file-obj)]
      (t/is (instance? Model model))
      (t/is (not (.isEmpty model)))
      (log/debugf "model instance: %s" (.toString model))
      (log/debugf "model size: %d" (.size model)))))


(t/deftest collection-type-test
    (t/testing "Collection type test for rdf:List"
      (let [model (->
                   (io/file "tests/resources/collections/type-list.ttl")
                   (m/loaded-model))]
        (t/is (not (.isEmpty model)))
        (let [collection-root (-> (.filter model nil (.createIRI vf "http://www.eexample.org/data#" "data") nil (r/context-array))
                                  (Models/object)
                                  (.get))]
          (t/is (some? collection-root))
          (log/debugf "root IRI: %s" (.stringValue collection-root))
          (let [submodel (.filter model collection-root nil nil (r/context-array))
                collection-type (cu/collection-type submodel)]
            (log/debugf "Collection type: %s" collection-type)
            (t/is (= collection-type RDF/LIST))))))

    (t/testing "Collection type test for rdfs:Container"
      (let [model (->
                   (io/file "tests/resources/collections/type-container.ttl")
                   (m/loaded-model))
            collection-root (-> (.filter model nil (.createIRI vf "http://www.eexample.org/data#" "data") nil (r/context-array))
                                (Models/object)
                                (.get))]
        (t/is (some? collection-root))
        (let [submodel (.filter model collection-root nil nil (r/context-array))
              collection-type (cu/collection-type submodel)]
          (log/debugf "Collection type: %s" collection-type)
          (t/is (= collection-type RDFS/CONTAINER))))))


(t/deftest rdf-coll-test
  (let [data-source (->
                     (io/file "tests/resources/collections/type-list.ttl")
                     (m/loaded-model))]
    (t/testing "simple rdf->seq"
      (let [collection-root (-> (m/rdf-filter data-source nil (.createIRI vf "http://www.eexample.org/data#" "data") nil)
                                (Models/object)
                                (.get))
            to-test (c/rdf->seq data-source collection-root [])]
        (t/is (not (empty? to-test)))
        (t/is (= 3 (count to-test)))
        (t/is (vector? to-test))
        (t/are [p x] (not (p x))
          set? to-test
          list? to-test)
        (log/debug (with-out-str (pp/pprint to-test)))))
    (t/testing "hetero rdf->seq"
      (let [collection-root (-> (m/rdf-filter data-source nil (.createIRI vf "http://www.eexample.org/data#" "data1") nil)
                                (Models/object)
                                (.get))
            to-test (c/rdf->seq data-source collection-root #{})]
        (t/is (not (empty? to-test)))
        (t/is (= 2 (count to-test)))
        (t/is (set? to-test))
        (t/are [p x] (not (p x))
          vector? to-test
          list? to-test)
        (log/debug (with-out-str (pp/pprint to-test)))))
    (t/testing "nested rdf"
      (let [collection-root (-> (m/rdf-filter data-source nil (.createIRI vf "http://www.eexample.org/data#" "data2") nil)
                                Models/object
                                .get)
            to-test (c/rdf->seq data-source collection-root [])]
        (let [pair1 (first to-test)
              triples (m/rdf-filter data-source pair1 nil nil)]
          #_(log/debugf "first pair: %s"  (c/rdf->seq data-source (first to-test) []))
          #_(log/debugf "\n%s\n" (m/print-model triples RDFFormat/NTRIPLES)))
        (let [i (first to-test)
              pair (c/rdf->seq data-source i [])]
          (t/is (= (-> pair
                       first
                       .getLabel) "var1"))
          (t/is (= (-> pair
                       second
                       .getLabel) "1.56")))
        (t/is (not (empty? to-test)))))))

(t/deftest rdf-protocols
  (let [md (->
            (io/file "tests/resources/beet.trig")
            m/loaded-model)]
    (t/testing "test protocols"
      (let [root (-> (m/rdf-filter md
                                   nil
                                   (.createIRI vf "file:/tmp2/" "beet-1.csvCountries")
                                   (.createLiteral vf "UnitedKingdom" XMLSchema/STRING))
                     Models/subjectIRI
                     .get)
            submodel (m/rdf-filter md root nil nil)
            gs (ModelHolder. root submodel)]
        (t/is (some? gs))
        (log/debugf "Instance: %5s\n" (binding [*print-dup* false] (println-str gs)))))))
