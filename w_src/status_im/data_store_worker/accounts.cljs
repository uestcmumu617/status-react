(ns status-im.data-store-worker.accounts
  (:require [status-im.data-store-worker.realm.accounts :as data-store]))

(defn get-all []
  (data-store/get-all-as-list))

(defn get-by-address [address]
  (data-store/get-by-address address))

(defn save [account update?]
  (data-store/save account update?))
