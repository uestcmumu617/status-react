(ns status-im.utils.random)

(def Chance              (js/require "chance"))

(defn timestamp []
  (.getTime (js/Date.)))

(def chance (Chance.))

(defn id []
  (str (timestamp) "-" (.guid chance)))

(defn rand-gen
  [seed]
  (Chance. seed))

(defn seeded-rand-int
  [gen n] (.integer gen #js {:min 0 :max (dec n)}))

(defn seeded-rand-nth
  [gen coll]
  (nth coll (seeded-rand-int gen (count coll))))
