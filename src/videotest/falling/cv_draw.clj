(ns videotest.falling.cv-draw
  (:import
   [org.opencv.core Core MatOfPoint Scalar]
   [java.util ArrayList]))


(defn draw-poly-with-pts
  "Draw a filled polygon with the given points and color."
  [img-mat color glyph-pts]
  (let [poly (MatOfPoint.)
        c (apply (fn [r g b a]
                   (Scalar. r g b a))
                 color)]
    (.fromList poly (ArrayList. glyph-pts))
    (Core/fillPoly img-mat (ArrayList. [poly]) c)))

(defn draw-line-with-pts
  [img-mat color-scalar [pt1 pt2]]
  (Core/line img-mat pt1 pt2 color-scalar 1 Core/LINE_4 0))

(defn draw-poly-outline-with-pts
  "Draw an outlined polygon with the given points and color."
  [img-mat color glyph-pts]
  (let [pts (take 3 (partition 2 1 (cycle glyph-pts)))
        c (apply (fn [r g b a]
                   (Scalar. r g b a))
                 color)]
    (dorun
     (map (partial draw-line-with-pts
                   img-mat
                   c)
          pts))))
