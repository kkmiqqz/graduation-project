package org.urbcomp.startdb.selfstar.compressor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class BatchUtils {
    public static int getWidthNeeded(long number) {
        // 由低向高查找
        if (number == 0) {
            return 0;
        }
        int bitCount = 0;
        while (number > 0) {
            bitCount++;
            number = number >>> 1; // 右移一位
        }
        return bitCount;

//        // 二分查找
//        if (number == 0) {
//            return 0;
//        }
//
//        int low = 1;
//        int high = 64; // 因为 long 类型有 64 位
//
//        while (low <= high) {
//            int mid = low + (high - low) / 2;
//
//            if ((number >>> mid) > 0) {
//                low = mid + 1;
//            } else {
//                high = mid - 1;
//            }
//        }
//
//        return low;
    }

    public static RLEresult RLE(int[] numbers, int cntWidth, int itemWidth) {
        int[] nums = new int[numbers.length];
        int[] cnts = new int[numbers.length];

        int index = 0;
        int current = numbers[0];
        int cnt = -1;
        int length = 0;
        int cntLimit = (1<<cntWidth) - 1;

        for (int num:numbers) {
            if (num != current) {
                nums[index] = current;
                cnts[index++] = cnt;
                current = num;
                cnt=0;
                length++;
            } else {
                if (cnt<cntLimit){
                    cnt++;
                }else{
                    nums[index] = current;
                    cnts[index++] = cnt;
                    cnt=0;
                    length++;
                }
            }
        }
        nums[index] = current;
        cnts[index++] = cnt;
        length++;

        nums = Arrays.copyOfRange(nums,0,length);
        cnts = Arrays.copyOfRange(cnts,0,length);

        return new RLEresult(nums,cnts,cntWidth,itemWidth);
    }

    /**
     * 供类似于signList的二值序列使用，对于与这类序列，若cntWidth恰好为maxWidth，则无需记录其值，也无需考虑这部分空间占用
     * @param numbers
     * @return
     */
    public static RLEresult AdaRLE(int[] numbers) {
        // 统计所有次数信息
        Map<Integer,Integer> cntMap = new HashMap<>();

        int current = numbers[0];
        int cnt = 0;
        int maxCnt = Integer.MIN_VALUE;

        for (int num:numbers) {
            if (num != current) {
                current = num;
                maxCnt = Math.max(maxCnt,cnt);
                cntMap.put(cnt,cntMap.getOrDefault(cnt,0)+1);
                cnt=1;
            } else {
                cnt++;
            }
        }
        maxCnt = Math.max(maxCnt,cnt);
        cntMap.put(cnt,cntMap.getOrDefault(cnt,0)+1);

        int maxWidth = getWidthNeeded(maxCnt-1);
        int minCost = Integer.MAX_VALUE;
        int cost = 0;
        int bestWidth = 0;

        for (int i=1;i<maxWidth;i++){
            for (Map.Entry<Integer, Integer> entry : cntMap.entrySet()){
                cost+= (int) ((i+1)*Math.ceil(entry.getKey()/(Math.pow(2,i)))*entry.getValue());
            }
            if (cost<minCost){
                minCost = cost;
                bestWidth = i;
            }
            cost=0;
        }
        // 单独考虑nonSplit的情况
        for (Map.Entry<Integer, Integer> entry : cntMap.entrySet()){
            cost+= maxWidth*entry.getValue();
        }
        if (cost<minCost-1){ // 1 bit for the record of first bit
            if (cost>=numbers.length){ // 负收益？
                return new RLEresult(numbers,new int[numbers.length],0,1);
            }
            minCost = cost;
            bestWidth = maxWidth;
            RLEresult res = RLE(numbers,bestWidth,1);
            res.nonSplit = true;
            return res;
        }else if (minCost>=numbers.length){ // 负收益？
            return new RLEresult(numbers,new int[numbers.length],0,1);
        }
        return RLE(numbers,bestWidth,1);
    }

    public static RLEresult AdaRLE(int[] numbers, int itemWidth) {
        // 统计所有次数信息
        Map<Integer,Integer> cntMap = new HashMap<>();

        int current = numbers[0];
        int cnt = 0;
        int maxCnt = Integer.MIN_VALUE;

        for (int num:numbers) {
            if (num != current) {
                current = num;
                maxCnt = Math.max(maxCnt,cnt);
                cntMap.put(cnt,cntMap.getOrDefault(cnt,0)+1);
                cnt=1;
            } else {
                cnt++;
            }
        }
        maxCnt = Math.max(maxCnt,cnt);
        cntMap.put(cnt,cntMap.getOrDefault(cnt,0)+1);

        int maxWidth = getWidthNeeded(maxCnt-1);
        int minCost = Integer.MAX_VALUE;
        int cost = 0;
        int bestWidth = 0;

        for (int i=1;i<=maxWidth;i++){
            for (Map.Entry<Integer, Integer> entry : cntMap.entrySet()){
                cost+= (int) ((i+itemWidth)*Math.ceil(entry.getKey()/(Math.pow(2,i)))*entry.getValue());
            }
            if (cost<minCost){
                minCost = cost;
                bestWidth = i;
            }
            cost=0;
        }

        if (minCost>=itemWidth* numbers.length){ // 负收益？
            return new RLEresult(numbers,new int[numbers.length],0,itemWidth);
        }

        return RLE(numbers,bestWidth,itemWidth);
    }
}

class RLEresult {
    public int[] nums;
    public int[] cnts;
    public int cntWidth;

    public int length;

    public int itemWidth;

    public boolean nonSplit = false;
    public RLEresult(int[] nums, int[] cnts, int cntWidth, int itemWidth){
        this.nums = nums;
        this.cnts = cnts;
        this.cntWidth = cntWidth;
        this.length = nums.length;
        this.itemWidth = itemWidth;
    }

    public void show(){
        System.out.println("Total cost : (cntWidth) "+cntWidth+" * (length) "+length+" = "+cntWidth*length);
        for (int i=0; i<length; i++){
            System.out.println(nums[i]+" : [cnt] "+cnts[i]);
        }
    }
}
