(ns status-im.data-store-worker.realm.schemas.account.v1.user-status
  (:require [taoensso.timbre :as log]))

(def schema {:name       :user-status
             :primaryKey :id
             :properties {:id               "string"
                          :whisper-identity {:type    "string"
                                             :default ""}
                          :status           "string"}})

(defn migration [_ _]
  (log/debug "migrating user-status schema"))
