(defproject videotest "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [quil "2.2.2"]
                 [opencv/opencv "2.4.10"]
                 [opencv/opencv-native "2.4.10"]]
  :jvm-opts ["-Xmx2G" "-Xms2G"]
  :profiles {:dev {:source-paths ["dev"]}}
  :injections [(clojure.lang.RT/loadLibrary org.opencv.core.Core/NATIVE_LIBRARY_NAME)])
