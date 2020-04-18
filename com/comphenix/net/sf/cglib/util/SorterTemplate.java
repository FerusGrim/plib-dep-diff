package com.comphenix.net.sf.cglib.util;

abstract class SorterTemplate
{
    private static final int MERGESORT_THRESHOLD = 12;
    private static final int QUICKSORT_THRESHOLD = 7;
    
    protected abstract void swap(final int p0, final int p1);
    
    protected abstract int compare(final int p0, final int p1);
    
    protected void quickSort(final int lo, final int hi) {
        this.quickSortHelper(lo, hi);
        this.insertionSort(lo, hi);
    }
    
    private void quickSortHelper(int lo, int hi) {
        while (true) {
            final int diff = hi - lo;
            if (diff <= 7) {
                break;
            }
            int i = (hi + lo) / 2;
            if (this.compare(lo, i) > 0) {
                this.swap(lo, i);
            }
            if (this.compare(lo, hi) > 0) {
                this.swap(lo, hi);
            }
            if (this.compare(i, hi) > 0) {
                this.swap(i, hi);
            }
            int j = hi - 1;
            this.swap(i, j);
            i = lo;
            final int v = j;
            while (true) {
                if (this.compare(++i, v) < 0) {
                    continue;
                }
                while (this.compare(--j, v) > 0) {}
                if (j < i) {
                    break;
                }
                this.swap(i, j);
            }
            this.swap(i, hi - 1);
            if (j - lo <= hi - i + 1) {
                this.quickSortHelper(lo, j);
                lo = i + 1;
            }
            else {
                this.quickSortHelper(i + 1, hi);
                hi = j;
            }
        }
    }
    
    private void insertionSort(final int lo, final int hi) {
        for (int i = lo + 1; i <= hi; ++i) {
            for (int j = i; j > lo && this.compare(j - 1, j) > 0; --j) {
                this.swap(j - 1, j);
            }
        }
    }
    
    protected void mergeSort(final int lo, final int hi) {
        final int diff = hi - lo;
        if (diff <= 12) {
            this.insertionSort(lo, hi);
            return;
        }
        final int mid = lo + diff / 2;
        this.mergeSort(lo, mid);
        this.mergeSort(mid, hi);
        this.merge(lo, mid, hi, mid - lo, hi - mid);
    }
    
    private void merge(final int lo, final int pivot, final int hi, final int len1, final int len2) {
        if (len1 == 0 || len2 == 0) {
            return;
        }
        if (len1 + len2 == 2) {
            if (this.compare(pivot, lo) < 0) {
                this.swap(pivot, lo);
            }
            return;
        }
        int len3;
        int first_cut;
        int second_cut;
        int len4;
        if (len1 > len2) {
            len3 = len1 / 2;
            first_cut = lo + len3;
            second_cut = this.lower(pivot, hi, first_cut);
            len4 = second_cut - pivot;
        }
        else {
            len4 = len2 / 2;
            second_cut = pivot + len4;
            first_cut = this.upper(lo, pivot, second_cut);
            len3 = first_cut - lo;
        }
        this.rotate(first_cut, pivot, second_cut);
        final int new_mid = first_cut + len4;
        this.merge(lo, first_cut, new_mid, len3, len4);
        this.merge(new_mid, second_cut, hi, len1 - len3, len2 - len4);
    }
    
    private void rotate(final int lo, final int mid, final int hi) {
        int lot = lo;
        int hit = mid - 1;
        while (lot < hit) {
            this.swap(lot++, hit--);
        }
        lot = mid;
        hit = hi - 1;
        while (lot < hit) {
            this.swap(lot++, hit--);
        }
        lot = lo;
        hit = hi - 1;
        while (lot < hit) {
            this.swap(lot++, hit--);
        }
    }
    
    private int lower(int lo, final int hi, final int val) {
        int len = hi - lo;
        while (len > 0) {
            final int half = len / 2;
            final int mid = lo + half;
            if (this.compare(mid, val) < 0) {
                lo = mid + 1;
                len = len - half - 1;
            }
            else {
                len = half;
            }
        }
        return lo;
    }
    
    private int upper(int lo, final int hi, final int val) {
        int len = hi - lo;
        while (len > 0) {
            final int half = len / 2;
            final int mid = lo + half;
            if (this.compare(val, mid) < 0) {
                len = half;
            }
            else {
                lo = mid + 1;
                len = len - half - 1;
            }
        }
        return lo;
    }
}
