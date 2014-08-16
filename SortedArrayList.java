import java.util.*;
import java.lang.Comparable;

/* 
	Copyright 2011 Kejardon
	Simple small thing to add objects to a vector, assuming the vector is presorted and comparable.
	Optimized for adding large objects in a vector of smaller objects.
	Not properly set up but screw it, it'll work how I want it to work.
*/
//This is really just about as good as the class can get without putting try/catch blocks everywhere in case of typecasting problems.
public class SortedArrayList<E extends Comparable<? super E>> extends ArrayList<E>
{
	public boolean workAsSet=false;
	public boolean workAsExactSet=false;
    public SortedArrayList(int initialCapacity) {
        super(initialCapacity);
    }
    public SortedArrayList() {
        super();
    }
    public SortedArrayList(Collection<? extends E> c) {
		super(c);
    }
	public boolean add(E O)
	{
		int i=0;
		if(workAsExactSet) for(i=size()-1;i>=0;i--)
		{
			if(O.equals(get(i))) return false;
			if(O.compareTo(get(i))>0)
				break;
		}
		else if(workAsSet) for(i=size()-1;i>=0;i--)
		{
			int result=O.compareTo(get(i));
			if(result==0) return false;
			if(result>0)
				break;
		}
		else for(i=size()-1;i>=0;i--)
			if(O.compareTo(get(i))>0)
				break;
		add(i+1, O);
		return true;
	}
	private int findAdjacentMatch(int i, E O)
	{
		if(O.equals(get(i))) return i;
		int j=i;
		while(++j<size() && O.compareTo(get(j))==0)
			if(O.equals(get(j))) return j;
		j=i;
		while(--j>=0 && O.compareTo(get(j))==0)
			if(O.equals(get(j))) return j;
		return -1;
	}
	//More efficent option for objects that may go anywhere in the vector.
	public boolean addRandom(E O)
	{
		int i=Collections.binarySearch(this, O);
		if(i<0) i=-i-1;
		else if(workAsExactSet)
		{
			int j=findAdjacentMatch(i,O);
			if(j>=0) return false;
		}
		else if(workAsSet)
		{
			return false;
		}
		add(i, O);
		return true;
	}
	public boolean contains(E O)
	{
		return (Collections.binarySearch(this, O)>=0);
	}
	public boolean contains(E O, boolean exact)
	{
		if(exact)
		{
			int i=Collections.binarySearch(this, O);
			if(i<0) return false;
			return (findAdjacentMatch(i, O)>=0);
		}
		return (Collections.binarySearch(this, O)>=0);
	}
	public boolean contains(Object O){return false;}
	public boolean remove(E O)
	{
		int i=Collections.binarySearch(this, O);
		if(i>=0)
		{
			if(workAsExactSet)
			{
				i=findAdjacentMatch(i,O);
				if(i<0) return false;
			}
			remove(i);
			return true;
		}
		return false;
	}
	public boolean remove(Object O){return false;}
	public int indexOf(E O)
	{
		int i=Collections.binarySearch(this, O);
		if(workAsExactSet && i>=0)
		{
			int j=findAdjacentMatch(i, O);
			if(j<=0) return -i;
			return j;
		}
		return i;
	}
	public int indexOf(Object O){return -1;}
	
	//Container class for making sortable objects
	//Call SortedList's instead, it's identical to the below
/*
	public static class SortableObject<E> implements Comparable<SortableObject>
	{
		public final E myObj=null;
		public final myInt=0;
		public SortableObject(E O, int i){myInt=i; myObj=O;}
		
		public int compareTo(SortableObject O)
		{ return myInt-O.myInt; }
		public boolean equals(Object O)
		{
			if(O instanceof SortableObject)
				return myInt==((SortableObject)O).myInt;
			return false;
		}
//		public int myInt(){return myInt;}
	}
*/
}
