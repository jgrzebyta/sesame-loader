(ns rdf4j.repository.sails
  (:import org.eclipse.rdf4j.lucene.spin.LuceneSpinSail
           [org.eclipse.rdf4j.sail.inferencer.fc DedupingInferencer ForwardChainingRDFSInferencer]
           org.eclipse.rdf4j.sail.memory.MemoryStore
           [org.eclipse.rdf4j.repository.sail SailRepository]
           org.eclipse.rdf4j.sail.nativerdf.NativeStore
           org.eclipse.rdf4j.sail.spin.SpinSail))

(defmulti make-sail-repository
  "Create instance of `SailRepository`.
  Additional map of options (`opts`) depends on sails hierarchy.

  TYPES
  - :memory
  - :native
  - :memory-rdfs
  - :native-rdfs
  - :memory-spin-lucene
  - :native-spin-lucene

  OPTIONS
  List of possible options:
  - :indexes -- pass value to `NativeStore/setTripleIndexes`
  - :parameters -- pass value to `LuceneSpinSail/setParameters`"
  {:arglists '([type datadir & [opts]])}
  (fn [type datadir & [opts]] (if (keyword? type)
                                type
                                (keyword type))) :default nil)

(defn- make-persisted-memory [datadir]
  (let [store (MemoryStore.)]
    (when datadir
      (.setPersist store true))
    store))


(defmethod make-sail-repository :memory [_ datadir & [opts]] (doto (-> (make-persisted-memory datadir)
                                                                       SailRepository.)
                                                               (.setDataDir datadir)))

(defmethod make-sail-repository :native [_ datadir & [opts]] (doto (-> (doto (NativeStore.)
                                                                        (.setTripleIndexes (or (:indexes opts) "spoc,posc")))
                                                                      SailRepository.)
                                                              (.setDataDir datadir)))

(defmethod make-sail-repository :memory-rdfs [_ datadir & [opts]] (doto (-> (make-persisted-memory datadir)
                                                                   DedupingInferencer.
                                                                   ForwardChainingRDFSInferencer.
                                                                   SailRepository.)
                                                           (.setDataDir datadir)))

(defmethod make-sail-repository :native-rdfs [_ datadir & [opts]] (doto (-> (doto (NativeStore.)
                                                                             (.setTripleIndexes (or (:indexes opts) "spoc,posc")))
                                                                           DedupingInferencer.
                                                                           ForwardChainingRDFSInferencer.
                                                                           SailRepository.)
                                                                   (.setDataDir datadir)))

(defmethod make-sail-repository :memory-spin-lucene [_ datadir & [opts]]
  (let [lucene-spin-sail (-> (make-persisted-memory datadir)
                             DedupingInferencer.
                             ForwardChainingRDFSInferencer.
                             SpinSail.
                             LuceneSpinSail.)
        parameters (:parameters opts)]
    (when parameters
      (.setParameters lucene-spin-sail parameters))
    (doto (-> lucene-spin-sail
              SailRepository.)
      (.setDataDir datadir))))

(defmethod make-sail-repository :native-spin-lucene [_ datadir & [opts]]
  (let [lucene-spin-sail (-> (doto (NativeStore.)
                               (.setTripleIndexes (or (:indexes opts) "spoc,posc")))
                             DedupingInferencer.
                             ForwardChainingRDFSInferencer.
                             SpinSail.
                             LuceneSpinSail.)
        parameters (:parameters opts)]
    (when parameters
      (.setParameters lucene-spin-sail parameters))

    (doto (-> lucene-spin-sail
              SailRepository.)
      (.setDataDir datadir))))

(defn sail-repository-types
  "Display a sequence of all repository types."
  []
  (doall (map #(name %) (filter some? (keys (methods rdf4j.repository.sails/make-sail-repository))))))

(defmethod make-sail-repository nil []
  (make-sail-repository :memory nil))
