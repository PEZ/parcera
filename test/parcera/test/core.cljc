(ns parcera.test.core
  (:require [clojure.pprint :as pprint]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check :as tc]
            [parcera.core :as parcera]
            [instaparse.core :as instaparse])
  #?(:cljs (:require-macros [parcera.slurp :refer [slurp]])))

(defn- roundtrip
  "checks parcera can parse and write back the exact same input code"
  [input]
  (= input (parcera/code (parcera/clojure input))))

(defn- valid?
  [input]
  (not (instaparse/failure? (parcera/clojure input))))

(defn- clear
  [input]
  (= 1 (count (instaparse/parses parcera/clojure input))))

(def validity
  "The grammar definition of parcera is valid for any clojure value. Meaning
  that for any clojure value, parcera can create an AST for it"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (= false (instaparse/failure? (parcera/clojure input)))))


(def symmetric
  "The read <-> write process of parcera MUST be symmetrical. Meaning
  that the AST and the text representation are equivalent"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (= input (parcera/code (parcera/clojure input)))))


(def unambiguous
  "The process of parsing clojure code yields consistent results. Meaning
  that any input should (but must not) only have 1 AST representation ... however
  I have found this is not always possible"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (= 1 (count (instaparse/parses parcera/clojure input)))))

(deftest simple
  (testing "character literals"
    (as-> "\\t" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "\\n" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "\\r" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "\\a" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "\\é" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "\\ö" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "\\ï" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "\\ϕ" input (is (= input (parcera/code (parcera/clojure input)))))))

(deftest data-structures
  (testing "grammar definitions"
    (let [result (tc/quick-check 200 validity)]
      (is (:pass? result)
          (str "read process failed at\n"
               (with-out-str (pprint/pprint result))))))

  (testing "clojure values"
    (let [result (tc/quick-check 200 symmetric)]
      (is (:pass? result)
          (str "read <-> write process yield different result. Failed at\n"
               (with-out-str (pprint/pprint result))))))

  (testing "very little ambiguity"
    (let [result (tc/quick-check 200 unambiguous)]
      (is (:pass? result)
          (str "high ambiguity case found. Please check the grammar to ensure "
               "high accuracy\n"
               (with-out-str (pprint/pprint result)))))))

(deftest unit-tests
  (testing "names"
    (as-> "foo" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "foo-bar" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "foo->bar" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "->" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "->as" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "föl" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "Öl" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "ϕ" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "❤️" input (is (and (valid? input) (roundtrip input) (clear input))))))


(deftest macros
  (testing "metadata"
    (as-> "^String [a b 2]" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "^\"String\" [a b 2]" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "^:string [a b 2]" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "^{:a 1} [a b 2]" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "^:hello ^\"World\" ^{:a 1} [a b 2]" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "discard"
    (as-> "#_[a b 2]" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "#_(a b 2)" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "#_{:a 1}" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "#_macros" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "regex"
    (as-> "#_\"[a b 2]\"" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "comments"
    (as-> ";[a b 2]" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> ";; \"[a b 2]\"" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "var quote"
    (as-> "#'hello/world" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "#'/" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "tag"
    (as-> "#hello/world [1 a \"3\"]" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "#hello/world {1 \"3\"}" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "keyword"
    (as-> "::hello/world [1 a \"3\"]" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "::hello" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "quote"
    (as-> "'hello/world" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "'hello" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "'/" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "backtick"
    (as-> "`hello/world" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "`hello" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "`/" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "unquote"
    (as-> "~hello/world" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "~(hello 2 3)" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "~/" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "quote splicing"
    (as-> "~@hello/world" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "~@(hello 2 b)" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "deref"
    (as-> "@hello/world" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "@hello" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "@/" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "anonymous function"
    (as-> "#(= (str %1 %2 %&))" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "namespaced map"
    (as-> "#::{:a 1 b 3}" input (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "#::hello{:a 1 b 3}" input (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "reader conditional"
    (as-> "#?(:clj Double/NaN :cljs js/NaN :default nil)" input
          (is (and (valid? input) (roundtrip input) (clear input))))
    (as-> "[1 2 #?@(:clj [3 4] :cljs [5 6])]" input
          (is (and (valid? input) (roundtrip input) (clear input))))))


(deftest bootstrap

  (testing "parcera should be able to parse itself"
    (let [input (slurp "./src/parcera/core.cljc")]
      (is (and (valid? input) (roundtrip input) (clear input)))))

  (testing "parcera should be able to parse its own test suite"
    (let [input (slurp "./test/parcera/test/core.cljc")]
      (is (and (valid? input) (roundtrip input) (clear input))))))


(deftest clojure$cript

  (testing "parcera should be able to parse clojure core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojure/master/src/clj/clojure/core.clj")]
      (time (is (= core-content (parcera/code (parcera/clojure core-content :optimize :memory)))))))

  (testing "parcera should be able to parse clojurescript core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojurescript/master/src/main/clojure/cljs/core.cljc")]
      (time (is (= core-content (parcera/code (parcera/clojure core-content :optimize :memory))))))))
