(ns videotest.triangles.static-rotation-core
  (:require
   [quil.core :as q]
   [quil.middleware :as m]
   [videotest.triangles.cv :as cv])
  (:import
   [org.opencv.core CvType Mat MatOfKeyPoint Point Size]
   [org.opencv.features2d FeatureDetector KeyPoint]
   [org.opencv.imgproc Imgproc]
   [java.nio ByteBuffer ByteOrder]))


(def CAM-SIZE (cv/camera-frame-size))
(def WIDTH  (int (:width  CAM-SIZE)))
(def HEIGHT (int (:height CAM-SIZE)))

;; pixCnt1 is the number of bytes in the pixel buffer
;; pixCnt2 is the number of integers in the PImage pixels buffer
(def PIX-CNT1 (* WIDTH HEIGHT 4))
(def PIX-CNT2 (* WIDTH HEIGHT))


(defn triangle-points
  "Saves origin points for each triangle, which is a vector pair of
   x and y positions in the image material [col row]."
  []
  (for [col-bin (range 0 NUM-COL-BINS)
        row-bin (range 0 NUM-ROW-BINS)
        :let [mat-col (+ (* col-bin MOSAIC-BIN-SIZE)
                         MOSAIC-BIN-SIZE-2)
              mat-row (+ (* row-bin MOSAIC-BIN-SIZE)
                         MOSAIC-BIN-SIZE-2)]
        :when (= 0 (mod col-bin 2))]
    [mat-col mat-row]))

(def MOSAIC-BIN-SIZE 12)
(def MOSAIC-BIN-SIZE-X2  (* MOSAIC-BIN-SIZE 2.0))
(def MOSAIC-BIN-SIZE-2   (/ MOSAIC-BIN-SIZE 2.0))
(def NEG-MOSAIC-BIN-SIZE (- MOSAIC-BIN-SIZE))
(def NUM-COL-BINS (/ WIDTH  MOSAIC-BIN-SIZE))
(def NUM-ROW-BINS (/ HEIGHT MOSAIC-BIN-SIZE))

(def PI_2   (/ Math/PI 2.0))
(def PI_3_2 (* 1.5 Math/PI))
(def ORIENTATIONS [0 PI_2 Math/PI PI_3_2])

(defn triangle-orientations
  "Radian orientations associated to triangle origin points."
  [triangle-points]
  (reduce (fn [memo [pt1 pt2]]
            (let [rot1 (nth ORIENTATIONS (rand-int 3))
                  rot2 (+ rot1 Math/PI)]
              (-> memo
                  (assoc-in [pt1] rot1)
                  (assoc-in [pt2] rot2))))
          {}
          (partition 2 triangle-points)))


;; bArray is the temporary byte array buffer for OpenCV cv::Mat.
;; iArray is the temporary integer array buffer for PImage pixels.
(defn setup []
  (q/frame-rate 60)
  (let [tri-pts (triangle-points)]
   {:b-array (byte-array PIX-CNT1)
    :i-array (int-array PIX-CNT2)
    :frame-mat (Mat. WIDTH HEIGHT CvType/CV_8UC3)
    :output-mat (Mat. WIDTH HEIGHT CvType/CV_8UC4)
    :gray-mat (Mat.)
    :rgba-mat (Mat.)
    :camera (cv/camera 0)
    :p-image (q/create-image WIDTH HEIGHT :rgb)
    :triangle-points tri-pts
    :triangle-orientations (triangle-orientations tri-pts)}))

(defn update-rgba [{:keys [rgba-mat frame-mat] :as state}]
  (assoc-in state [:rgba-mat] (cv/BGR->RGBA! frame-mat rgba-mat)))

(defn update [state]
  (-> state
      (cv/update-frame)
      #_(update-triangle-orientations)
      
      (update-rgba)
      #_(cv/update-p-image)
      ))

(defn draw-mosaic-glyph [color
                         rotation
                         mat-col mat-row]
  (apply q/fill color)
  (apply q/stroke color)
  
  (q/with-translation [(+ mat-col MOSAIC-BIN-SIZE)
                       (+ mat-row MOSAIC-BIN-SIZE)]
    (q/with-rotation [rotation]
      (q/triangle NEG-MOSAIC-BIN-SIZE NEG-MOSAIC-BIN-SIZE
                  NEG-MOSAIC-BIN-SIZE     MOSAIC-BIN-SIZE
                      MOSAIC-BIN-SIZE     MOSAIC-BIN-SIZE))))

(defn draw-mosaic-pair
  [triangle-orientations color-mat [pt1 pt2]]
  (let [color-fn (fn [mat-col mat-row]
                   (let [c (.get color-mat mat-row mat-col)]
                     (if (< 0 (count c))
                       (vec c)
                       [0 0 0])))
        [mat-col1 mat-row1] pt1
        [mat-col2 mat-row2] pt2
        c1 (color-fn mat-col1 mat-row1)
        c2 (color-fn mat-col2 mat-row2)
        rotation1 (triangle-orientations pt1)
        rotation2 (triangle-orientations pt2)]
    (draw-mosaic-glyph c1 rotation1 mat-col1 mat-row1)
    (draw-mosaic-glyph c2 rotation2 mat-col1 mat-row1)))

(defn draw-mosaic
  [{:keys [rgba-mat triangle-points triangle-orientations]}]
  (q/push-matrix)
  (q/push-style)
  ;; (q/no-stroke)
  (q/stroke-weight 2.0) ; remove borders
  (dorun
   (map (partial draw-mosaic-pair
                 triangle-orientations
                 rgba-mat)
        (partition 2 triangle-points)))
  (q/pop-style)
  (q/pop-matrix))

(defn draw [state]
  (let [{:keys [p-image frame-mat]} state]
    (q/background 0)
    (q/push-matrix)
    (q/translate WIDTH 0)
    (q/scale -1 1)
    #_(when p-image
      (q/image p-image 0 0)
      #_(draw-centroids state))
    (when-not (cv/mat-empty? frame-mat)
      (draw-mosaic state))
    (q/pop-matrix)))

(defn on-close
  ([{:keys [camera]}]
     (println "closing sketch and releasing camera...")
     (when-not (nil? camera)
       (.release camera))))

(q/defsketch videotest
  :title "Video Test"
  :size [WIDTH HEIGHT]
  :setup setup
  :update update
  :draw draw
  :on-close on-close
  :middleware [m/fun-mode])
