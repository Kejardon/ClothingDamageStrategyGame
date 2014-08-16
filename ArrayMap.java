import java.util.*;

/*
 * Armour Battle
 * by Kejardon and LonelyWorld, from tgchan
 * Copyright 2013
 */


public class ArrayMap<K, J> implements Map<K, J>
{
	public ArrayMap()
	{
		listKey=new ArrayList();
		listValue=new ArrayList();
	}
	public ArrayMap(int size)
	{
		listKey=new ArrayList(size);
		listValue=new ArrayList(size);
	}

	ArrayList<K> listKey;
	ArrayList<J> listValue;
	
	public Set<Entry<K, J>> entrySet()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int size() { return listKey.size(); }
	public boolean isEmpty() { return listKey.isEmpty(); }
	public boolean containsKey(Object key)
	{
		if(key==null)
		{
			for(int i=0;i<listKey.size();i++)
			{
				K e=listKey.get(i);
				if(e==null) return true;
			}
		}
		else
			for(int i=0;i<listKey.size();i++)
			{
				K e=listKey.get(i);
				if(key.equals(e)) return true;
			}
		return false;
	}

	public boolean containsValue(Object value)
	{
		if(value==null)
		{
			for(int i=0;i<listValue.size();i++)
			{
				J e=listValue.get(i);
				if(e==null) return true;
			}
		}
		else
			for(int i=0;i<listValue.size();i++)
			{
				J e=listValue.get(i);
				if(value.equals(e)) return true;
			}
		return false;
	}

	public J get(Object key)
	{
		if(key==null)
		{
			for(int i=0;i<listKey.size();i++)
			{
				K e=listKey.get(i);
				if(e==null) return listValue.get(i);
			}
		}
		else
			for(int i=0;i<listKey.size();i++)
			{
				K e=listKey.get(i);
				if(key.equals(e)) return listValue.get(i);
			}
		return null;
	}

	public J put(K key, J value)
	{
		if(key==null)
		{
			for(int i=0;i<listKey.size();i++)
			{
				K e=listKey.get(i);
				if(e==null)
					return listValue.set(i, value);
			}
		}
		else
			for(int i=0;i<listKey.size();i++)
			{
				K e=listKey.get(i);
				if(key.equals(e))
					return listValue.set(i, value);
			}
		listKey.add(key);
		listValue.add(value);
		return null;
	}

	public J remove(Object key)
	{
		if(key==null)
		{
			for(int i=0;i<listKey.size();i++)
			{
				K e=listKey.get(i);
				if(e==null)
				{
					listKey.remove(i);
					return listValue.remove(i);
				}
			}
		}
		else
			for(int i=0;i<listKey.size();i++)
			{
				K e=listKey.get(i);
				if(key.equals(e))
				{
					listKey.remove(i);
					return listValue.remove(i);
				}
			}
		return null;
	}

	public void putAll(Map<? extends K, ? extends J> m)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void clear()
	{
		listKey.clear();
		listValue.clear();
	}

	public Set<K> keySet()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Collection<J> values()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
