package util;

import java.util.Arrays;

/**
 * Adoption of Java ArrayList for primitive integer.
 * @author schaeler
 *
 */
public final class MyArrayList {
	public int[] ARRAY;
	static final int DEFAULT_INITIAL_SIZE = 16;//guessed
	private static final int EQUAL = 0;
	int capacity;
	int writeHere = 0;
	
	public MyArrayList(){
		this(DEFAULT_INITIAL_SIZE);
	}
	
	public MyArrayList(int initialSize){
		ARRAY = new int[initialSize];
		capacity = initialSize;
	}
	public MyArrayList(int[] array){
		ARRAY = array;
		capacity = array.length;
	}
	
	public void add(int element){
		if(writeHere==capacity){//esure capacity if full
			int[] newArray = new int[2*capacity];
			System.arraycopy(ARRAY, 0, newArray, 0, capacity);
			capacity *= 2;
			this.ARRAY = newArray;
		}
		ARRAY[writeHere++] = element;
	}
	public int get(int index){
		return ARRAY[index];
	}
	public int size(){
		return writeHere;
	}

	/** Virtual delete, i.e., does not free the associated memory.*/
	public void clear() {
		writeHere = 0;
	}

	public int[] toSortedArray() {
		int[] myArray = new int[this.size()];
		System.arraycopy(ARRAY, 0, myArray, 0, size());
		Arrays.sort(myArray);
		return myArray;
	}

	public void ensureCapacity(final int requiredCapacity) {
		if(requiredCapacity>this.capacity){
			int[] newArray = new int[2*requiredCapacity];
			System.arraycopy(ARRAY, 0, newArray, 0, capacity);
			capacity *= 2;
			this.ARRAY = newArray;
		}
	}

	public void sort(){
		if(writeHere==0){//empty
			return;
		}
		Arrays.sort(ARRAY,0,writeHere);
	}
	
	public void addAll(final int[] array, final int start, final int stop) {
		final int length = stop-start;
		ensureCapacity(length);
		System.arraycopy(array, start, this.ARRAY, 0, length);
		this.writeHere = length;
	}
	
	/**
	 * returns first difference
	 * @param otherList
	 * @return
	 */
	int compare(MyArrayList otherList){
		int[] l1 = this.toSortedArray();
		int[] l2 = otherList.toSortedArray();
		
		int minLength = Math.min(l1.length, l2.length);
		for(int i=0;i<minLength;i++){
			if(l1[i]!=l2[i]){
				System.out.println("First differnce @i="+i+" l1_i="+l1[i]+" l2_i="+l2[i]);
				return i;
			}
		}
		return EQUAL;
	}
	
	public void add(final MyArrayList toAdd){
		final int NEW_SIZE = this.size()+toAdd.size();
		ensureCapacity(NEW_SIZE);
		int[] from = toAdd.ARRAY;
		int[] to = this.ARRAY;
		System.arraycopy(from, 0, to, this.writeHere, toAdd.size());
		this.writeHere = NEW_SIZE;
	}
	
	public String toString(){
		return size()+"";
	}

	public String toString(int elmsToDisplay){
		String ret = "";
		for(int elem=0;elem<elmsToDisplay;elem++){
			ret+=" "+this.ARRAY[elem]+",";
		}
		return ret;
	}
	/*public boolean contains(final int value) {
		final int offset = Arrays.binarySearch(ARRAY, 0, writeHere, value);
		if(offset >= 0 && offset <writeHere){
			return true;
		}
		return false;
	}*/
	
	public void intersect(MyArrayList toIntersect){
		final int cand_length 		= this.size();
		final int to_merge_length 	= toIntersect.size();
		final int[] candidates 	= this.ARRAY;
		final int[] toMerge 	= toIntersect.ARRAY;
		
		this.clear();//the clear is only virtual, i.e., data is still there, but writepointer is at front and can use it to write elements that are in both list at the currently next position. Recap, there are at most this.size() elems.
		
		int i = 0, j = 0;
		while (i < cand_length && j < to_merge_length){
			if(candidates[i] < toMerge[j]){//elem in candidates not found
				i++;
			}else if (toMerge[j] < candidates[i]){
				j++;
			}else{// if arr1[i] == arr2[j]
				candidates[writeHere++] = candidates[i];//this element remains
				j++;
				i++;
			}
		}
	}

	/** Physical delete, i.e., does free the associated memory.*/
	public void delete() {
		this.writeHere = -1;
		this.capacity = -1;
		this.ARRAY = null;//dear garbage collector, please really free the memory ...
	}
}
