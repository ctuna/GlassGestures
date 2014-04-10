package com.achaldave.myapplication2.app;

import android.opengl.Matrix;
import android.util.Log;

/**
 * Created by Achal on 4/3/14.
 */
public class RotationMatrix {
    public float[][] matrix = new float[4][4];

    public RotationMatrix() {
        setIdentity();
    }

    /**
     * Creates a RotationMatrix from a row major vector
     * @param mat - row major vector
     */
    public RotationMatrix(float[] mat) {
        setMatrix(mat);
    }

    public RotationMatrix(float[][] mat) {
        this.matrix = mat;
    }

    public RotationMatrix(RotationMatrix other) {
        Log.d("Finder", "Cloning matrix");
        for (int i = 0; i < 4; ++i)
            this.matrix[i] = other.matrix[i].clone();
    }

    public static RotationMatrix fromColMajor(float[] mat) {
        RotationMatrix tmp = new RotationMatrix(mat);
        tmp.transpose();
        return tmp;
    }

    /**
     * Sets my matrix from a row major float vector
     * @param mat: matrix in row major format
     */
    public void setMatrix(float[] mat) {
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                this.matrix[i][j] = mat[i*4+j];
            }
        }
    }

    public void transpose() {
        float[][] tmp = new float[4][4];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                tmp[i][j] = this.matrix[j][i];
            }
        }
        this.matrix = tmp;
    }

    public RotationMatrix transposed() {
        RotationMatrix out = new RotationMatrix(this);
        out.transpose();
        return out;
    }

    public void invert() { transpose(); }
    public RotationMatrix inverted() { return transposed(); }

    public void setIdentity() {
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                if (i == j)
                    this.matrix[i][j] = 1;
                else
                    this.matrix[i][j] = 0;
            }
        }
    }

    public float distance(RotationMatrix other) {
        float dist = 0;
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                dist += Math.abs(this.matrix[i][j] - other.matrix[i][j]);
            }
        }
        return dist;
    }

    public float[] toRowMajor() {
        float[] tmp = new float[16];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                tmp[i*4+j] = this.matrix[i][j];
            }
        }
        return tmp;
    }

    public float[] toColMajor() {
        float[] tmp = new float[16];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                tmp[i*4+j] = this.matrix[j][i];
            }
        }
        return tmp;
    }

    public RotationMatrix multiply(RotationMatrix other) {
        float[] thisColMajor  = this.toColMajor();
        float[] otherColMajor = other.toColMajor();
        float[] result = new float[16];
        Matrix.multiplyMM(result, 0, thisColMajor, 0, otherColMajor, 0);
        return RotationMatrix.fromColMajor(result);
    }

    public String repr() {
        String out = "[";
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                out += Float.toString(this.matrix[i][j]);
                if (j != 3) out += ", ";
            }
            if (i != 3) out += "\n";
        }
        out += "]";
        return out;
    }

}
