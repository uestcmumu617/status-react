(ns status-im.data-store-worker.core
  (:require [status-im.data-store-worker.realm.core :as data-source]
            [taoensso.timbre :as log]))

(defn do-init []
  (log/info "do-init")
  (data-source/reset-account))

(defn do-change-account [{:keys [address new-account? handler]}]
  (log/info "do-change-account")
  (data-source/change-account address new-account? handler))
