;;; Records the results of runs to `ici-recorder`.
;;;
;;; We want to record two types of information:
;;;   1. The initial run configuration
;;;   2. The results of a generation
;;; Each run has a UUID. Each generation is recorded with a reference to this
;;; UUID and it's index.
;;;
;;; We build up the data we want to send for the generation/run over time
;;; and then send it off.
(ns clojush.pushgp.record
  (:require [ici-recorder]))

(def data (atom {}))

;; called at the begining of a new run.
;;
;; Resets the state and creates UUID
(defn new-run! []
  (reset! data {:uuid (java.util.UUID/randomUUID)
                :start-time (java.time.Instant/now)}))
;; Stores a configuration options for the run, for the sequence of `ks` and value `v`
(defn config-data! [ks v]
  (swap! data assoc-in (cons :configuration ks) v)
  v)
;; Records the run configuration with `ici-recorder`
(defn end-config! []
  (let [{:keys [configuration uuid]} @data]
    (ici-recorder/record-run uuid configuration)))

;; Called at the begining of a generation
(defn new-generation! [index]
  (swap!
    data
    (fn [m]
      (assoc
        (select-keys m [:uuid])
        :index index))))

;; stores some data about the generation
(defn generation-data! [ks v]
  (swap! data assoc-in (cons :generation ks) v)
  v)

;; records the generation with `ici-recorder`
(defn end-generation! []
  (let [{:keys [generation uuid index]} @data]
    (ici-recorder/record-generation uuid index generation)))
