package com.github.smk7758.GetSubstituteFingerPoint;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import com.github.smk7758.GetSubstituteFingerPoint.Main.LogLevel;

public class CalculatePoint {
	private Mat cameraMatrix;
	private MatOfDouble distortionCoefficients;
	double focus;

	public CalculatePoint(CameraParameter cameraParameter) {
		cameraMatrix = cameraParameter.cameraMatrix;
		distortionCoefficients = cameraParameter.distortionCoefficients_;

		double[] tmp_f_x = new double[cameraMatrix.channels()], tmp_f_y = new double[cameraMatrix.channels()];
		cameraMatrix.get(0, 0, tmp_f_x);
		cameraMatrix.get(1, 1, tmp_f_y);
		double f_x = tmp_f_x[0], f_y = tmp_f_y[0];
		focus = (f_x + f_y) / 2;
	}

	/**
	 * 指先の点の空間座標を変換して、接地判定を行う。
	 *
	 * @param vectorA_ 指先の点(Z=f)
	 */
	public void process(Point fingerPoint, Mat rotationVector, Mat translationVector, Mat outputMat) {
		Main.debugLog("Rv: " + rotationVector.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		Mat rotationMatrix = new Mat();
		Calib3d.Rodrigues(rotationVector, rotationMatrix);
		Main.debugLog("R: " + rotationMatrix.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		Mat translationVector_ = convertVerticalTranslationVectorHorizontal(translationVector);

		// A_, 指先の点(Z=f)
		Mat vectorA_ = new Mat(3, 1, CvType.CV_64F);
		// vectorA_.put(0, 0, new double[] { focus, fingerPoint.y, fingerPoint.x }); //TODO

		Main.debugLog("vectorA_: " + vectorA_.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		// create Nw
		Mat vectorNw = new Mat(3, 1, CvType.CV_64F);
		vectorNw.put(0, 0, new double[] { 0, 1, 0 });

		// Nw → N
		Mat vectorN = new Mat();
		// CalculatePoint.convertToWorldCoordinate(vectorNw, rotationMatrix, translationVector_, vectorN);
		// vectorN = normalizeVector(vectorN);
		Main.debugLog("vectorN: " + vectorN.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		double k_0 = vectorN.dot(translationVector);
		double k_1 = vectorN.dot(vectorA_);
		double k = vectorN.dot(translationVector) / vectorN.dot(vectorA_);

		Main.debugLog("k_0:  " + k_0 + ", k_1: " + k_1 + ", k: " + k, LogLevel.DEBUG, "process | CalculatePoint"); // TODO

		// Mat vectorA = new Mat(3, 1, CvType.CV_64F);
		Mat vectorA = vectorA_.mul(Mat.ones(vectorA_.size(), vectorA_.type()), k);

		Main.debugLog("A_: " + vectorA_.dump(), LogLevel.DEBUG, "process | CalculatePoint");
		Main.debugLog("A:  " + vectorA.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		Mat vectorAw = new Mat(3, 1, CvType.CV_64F);

		// System.out.println(CvType.typeToString(rotationMatrix.type()));

		CalculatePoint.unconvertToWorldCoordinate(vectorA, rotationVector, translationVector_, vectorAw);

		Main.debugLog("Aw: " + vectorAw.dump(), LogLevel.DEBUG, "process | CalculatePoint"); // TODO

		int l = 20;
		Mat vectorBw = vectorAw.clone();
		Main.debugLog("Bw: " + vectorBw.dump(), LogLevel.DEBUG, "process | CalculatePoint");
		Main.debugLog("item: " + vectorBw.get(2, 0)[0], LogLevel.DEBUG, "process | CalculatePoint");

		vectorBw.put(0, 2, vectorBw.get(2, 0)[0] + l);

		Point3 pointA = new Point3(vectorA.get(0, 0)[0], vectorA.get(1, 0)[0], vectorA.get(2, 0)[0]);
		MatOfPoint3f pointsSrc = new MatOfPoint3f(pointA);
		MatOfPoint2f pointsDst = new MatOfPoint2f();
		Calib3d.projectPoints(pointsSrc, rotationVector, translationVector,
				cameraMatrix, distortionCoefficients, pointsDst);

		Main.debugLog("pointDst: " + pointsDst.get(0, 0)[0] + ", " + pointsDst.get(0, 0)[1],
				LogLevel.DEBUG, "process - CalculatePoint");

		Imgproc.circle(outputMat, new Point(pointsDst.get(0, 0)), 10, new Scalar(255, 255, 255));
		// System.out.println(pointsDst.dump()); // TODD
	}

	/**
	 * 指先の点の空間座標を変換して、接地判定を行う。(改良版？)
	 *
	 * @param vectorA_ 指先の点(Z=f)
	 */
	public void process_(Mat vectorA_, Mat rotationMatrix, Mat translationVector, Mat outputMat) {
		// Main.debugLog("Rv: " + rotationVector.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		// Mat rotationMatrix = new Mat();
		// Calib3d.Rodrigues(rotationVector, rotationMatrix);
		Main.debugLog("R: " + rotationMatrix.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		// Mat translationVector_ = convertVerticalTranslationVectorHorizontal(translationVector);
		Mat translationVector_ = translationVector;

		// A_, 指先の点(Z=f)
		// Mat vectorA_ = new Mat(3, 1, CvType.CV_64F);
		// vectorA_.put(0, 0, new double[] { focus, fingerPoint.y, fingerPoint.x }); //TODO

		Main.debugLog("vectorA_: " + vectorA_.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		// create Nw
		Mat vectorNw = new Mat(3, 1, CvType.CV_64F);
		vectorNw.put(0, 0, new double[] { 0, 1, 0 });

		// Nw → N
		Mat vectorN = new Mat();

		// Nwベクトルの座標軸的な角度の変換 N = R*Nw
		convertToWorldCoordinate(vectorNw, rotationMatrix, new Mat(), vectorN);

		double k_0 = vectorN.dot(translationVector);
		double k_1 = vectorN.dot(vectorA_);
		double k = vectorN.dot(translationVector) / vectorN.dot(vectorA_);

		Main.debugLog("k_0:  " + k_0 + ", k_1: " + k_1 + ", k: " + k, LogLevel.DEBUG, "process | CalculatePoint"); // TODO

		Mat vectorA = vectorA_.mul(Mat.ones(vectorA_.size(), vectorA_.type()), k);

		Main.debugLog("A:  " + vectorA.dump(), LogLevel.DEBUG, "process | CalculatePoint");
		Main.debugLog("A_: " + vectorA_.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		Mat vectorAw = new Mat(3, 1, CvType.CV_64F);

		CalculatePoint.unconvertToWorldCoordinate_(vectorA, rotationMatrix, translationVector_, vectorAw);

		Main.debugLog("Aw: " + vectorAw.dump(), LogLevel.DEBUG, "process | CalculatePoint"); // TODO

		int l = 5; // TODO
		Mat vectorBw = new Mat();
		Mat vectorNw_l = vectorNw.mul(Mat.ones(vectorNw.size(), vectorNw.type()), l);
		Core.add(vectorAw, vectorNw_l, vectorBw);

		Main.debugLog("vecrotNw_l: " + vectorNw_l.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		Main.debugLog("Bw: " + vectorBw.dump(), LogLevel.DEBUG, "process | CalculatePoint");

		Point3 pointAw = new Point3(vectorAw.get(0, 0)[0], vectorAw.get(1, 0)[0], vectorAw.get(2, 0)[0]);
		Point3 pointBw = new Point3(vectorBw.get(0, 0)[0], vectorBw.get(1, 0)[0], vectorBw.get(2, 0)[0]);
		MatOfPoint3f pointsSrc = new MatOfPoint3f(pointAw, pointBw);
		MatOfPoint2f pointsDst = new MatOfPoint2f();
		Calib3d.projectPoints(pointsSrc, rotationMatrix, translationVector,
				cameraMatrix, distortionCoefficients, pointsDst);

		Main.debugLog("pointDst: " + pointsDst.dump(), LogLevel.DEBUG);

		// Imgproc.circle(outputMat, new Point(pointsDst.get(0, 0)), 10, new Scalar(255, 255, 255));
		// System.out.println(pointsDst.dump()); // TODD
	}

	/**
	 * Word座標への変換をする cf: matDst = R*matSrc + t
	 */
	public static void convertToWorldCoordinate(Mat matSrc, Mat rotationMatrix, Mat translationVector, Mat matDst) {
		// Main.debugLog("t: " + translationVector.t().dump(), LogLevel.DEBUG, "convertToWorldCoordinate");

		Core.gemm(rotationMatrix, matSrc, 1, translationVector.t(), 1, matDst);

		// ImgProcessUtil.multiplicationMat(rotationMatrix, matSrc, matDst);
		// Core.multiply(rotationVoctor, matSrc, matDst);
		// Core.add(matDst, translationVector, matDst);
	}

	/**
	 * @param matSrc
	 * @param rotationVector
	 * @param translationVector
	 * @param matDst
	 */
	public static void unconvertToWorldCoordinate(Mat matSrc, Mat rotationVector, Mat translationVector, Mat matDst) {
		// Main.debugLog("matSrc: " + matSrc.dump(), LogLevel.DEBUG, "convertToWorldCoordinate");
		// Main.debugLog("R: " + rotationVector.dump(), LogLevel.DEBUG, "convertToWorldCoordinate");
		// Main.debugLog("t: " + translationVector.t().dump(), LogLevel.DEBUG, "convertToWorldCoordinate");

		// Mat rotationMatrix = new Mat();
		// Core.invert(rotationMatrix, rotationMatrix_0); //回転行列の逆行列はこれでも良い。
		// Imgproc.invertAffineTransform(rotationMatrix_0, rotationMatrix_0); // ゴミ

		// Rodrigues行列の反転 (逆変換のため)
		Mat rotationVector_0 = rotationVector.mul(Mat.eye(rotationVector.size(), rotationVector.type()), -1);

		// 回転行列の逆行列をRodriges行列の反転したものから生成
		Mat rotationMatrix_0 = new Mat();
		Calib3d.Rodrigues(rotationVector_0, rotationMatrix_0);
		Main.debugLog("R0: " + rotationMatrix_0.dump(), LogLevel.DEBUG, "convertToWorldCoordinate");

		// tベクトルを転置して引き算して出力？(転置しないとならない理由は何？)
		Core.subtract(matSrc, translationVector.t(), matDst);

		// System.out.println("Rv0: " + rotationVector_0.dump() + ", " + rotationVector_0.height());

		Main.debugLog("matDst(before, multi): " + matDst.dump(), LogLevel.DEBUG, "convertToWorldCoordinate");

		// matDstは再利用物
		Core.gemm(rotationMatrix_0, matDst, 1, new Mat(), 0, matDst);
		// ImgProcessUtil.multiplicationMat(rotationVector_0, matDst, matDst);
		// Core.multiply(rotationMatrix_0, matDst, matDst); // TODO

		Main.debugLog("matDst(after, multi): " + matDst.dump(), LogLevel.DEBUG, "convertToWorldCoordinate");
	}

	/**
	 * @param matSrc
	 * @param rotationVector
	 * @param translationVector
	 * @param matDst
	 */
	public static void unconvertToWorldCoordinate_(Mat matSrc, Mat rotationMatrix, Mat translationVector, Mat matDst) {
		// Main.debugLog("matSrc: " + matSrc.dump(), LogLevel.DEBUG, "convertToWorldCoordinate");
		// Main.debugLog("t: " + translationVector.t().dump(), LogLevel.DEBUG, "convertToWorldCoordinate");

		Mat rotationMatrix_invert = rotationMatrix.inv(); // 逆行列だと思う

		Main.debugLog("R_inv: " + rotationMatrix_invert.dump(), LogLevel.DEBUG, "convertToWorldCoordinate");

		// tベクトルを転置して引き算して出力？(転置しないとならない理由は何？)
		Core.subtract(matSrc, translationVector.t(), matDst);

		// matDstは再利用物
		Core.gemm(rotationMatrix_invert, matDst, 1, new Mat(), 0, matDst);
	}

	/**
	 * 縦ベクトル(Vertical)のtranslationVectorの横ベクトル(Horizontal)を返す。← それ転置では？
	 *
	 * @param verticalTranslationVector
	 * @return
	 */
	private static Mat convertVerticalTranslationVectorHorizontal(Mat verticalTranslationVector) {
		Mat horizontalTranslationVector = new Mat(1, 3, CvType.CV_64F);
		for (int channel = 0; channel < verticalTranslationVector.channels(); channel++) {
			horizontalTranslationVector.put(0, channel, verticalTranslationVector.get(0, 0)[channel]);
		}
		return horizontalTranslationVector;
	}

	/**
	 * 単位ベクトル(unit vector)を返します。
	 */
	public static Mat normalizeVector(Mat vector) {
		Mat dst = new Mat(vector.size(), vector.type());
		Core.normalize(vector, dst, 1.0, 0, Core.NORM_L2);
		return dst;
	}
}
