(ns metabase.util.i18n.impl-test
  (:require [clojure.test :refer :all]
            [metabase.test :as mt]
            [metabase.util.i18n.impl :as impl]))

(deftest locale-test
  (testing "Should be able to coerce various types of objects to Locales"
    (doseq [x [(java.util.Locale/forLanguageTag "en-US")
               "en-US"
               "en_us"
               :en/US
               :en-US
               :en_us]]
      (testing (format "^%s %s" (some-> x class .getCanonicalName) (pr-str x))
        (is (= (java.util.Locale/forLanguageTag "en-US")
               (impl/locale x))))))

  (testing "nil"
    (is (= nil
           (impl/locale nil)))))

(deftest parent-locale-test
  (doseq [[locale expected] {nil                                       nil
                             :es                                       nil
                             "es"                                      nil
                             (java.util.Locale/forLanguageTag "es")    nil
                             "es-MX"                                   (java.util.Locale/forLanguageTag "es")
                             "es_MX"                                   (java.util.Locale/forLanguageTag "es")
                             :es/MX                                    (java.util.Locale/forLanguageTag "es")
                             (java.util.Locale/forLanguageTag "es-MX") (java.util.Locale/forLanguageTag "es")}]
    (testing locale
      (is (= expected
             (impl/parent-locale locale))))))

(deftest graceful-fallback-test
  (testing "If a resource bundle doesn't exist, we should gracefully fall back to English"
    (is (= "Translate me 100"
           (impl/translate "zz" "Translate me {0}" 100)))))

(deftest translate-test
  (mt/with-mock-i18n-bundles {"es"    {"Your database has been added!"  "¡Tu base de datos ha sido añadida!"
                                         "I''m good thanks"               "Está bien, gracias"
                                         "must be {0} characters or less" "deben tener {0} caracteres o menos"}
                                "es-MX" {"I''m good thanks" "Está muy bien, gracias"}}
    (testing "Should be able to translate stuff"
      (is (= "¡Tu base de datos ha sido añadida!"
             (impl/translate "es" "Your database has been added!"))))

    (testing "should be able to use language-country Locale if available"
      (is (= "Está muy bien, gracias"
             (impl/translate "es-MX" "I''m good thanks"))))

    (testing "should fall back from `language-country` Locale to `language`"
      (is (= "¡Tu base de datos ha sido añadida!"
             (impl/translate "es-MX" "Your database has been added!"))))

    (testing "Should fall back to English if no bundles/translations exist"
      (is (= "abc 123 wow"
             (impl/translate "ok" "abc 123 wow")
             (impl/translate "es" "abc 123 wow"))))

    (testing "format strings with arguments"
      (is (= "deben tener 140 caracteres o menos"
             (impl/translate "es" "must be {0} characters or less" 140))))))

(deftest translate-error-handling-test
  (mt/with-mock-i18n-bundles {"ba-DD" {"Bad translation {0}" "BaD TrAnSlAtIoN {a}"}}
    (testing "Should fall back to original format string if translated one is busted"
      (is (= "Bad translation 100"
             (impl/translate "ba-DD" "Bad translation {0}" 100))))

    (testing "if the original format string is busted, should just return format-string as-is (better than nothing)"
      (is (= "Bad original {a}"
             (impl/translate "ba-DD" "Bad original {a}" 100))))))
