(ns status-im.data-store-worker.realm.schemas.account.v8.local-storage)

(def schema {:name       :local-storage
             :primaryKey :chat-id
             :properties {:chat-id "string"
                          :data    {:type    "string"
                                    :default "{}"}}})
