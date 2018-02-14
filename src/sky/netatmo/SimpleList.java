package sky.netatmo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

public class SimpleList<T> implements List<T>,RandomAccess
{
    private final ArrayList<T> list;

    SimpleList(ArrayList<T> list)
    {
        this.list=list;
    }

    public int size()
    {
        return list.size();
    }

    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    public boolean contains(Object o)
    {
        return list.contains(o);
    }

    public Iterator<T> iterator()
    {
        return list.iterator();
    }

    public Object[] toArray()
    {
        return list.toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        return list.toArray(a);
    }

    public boolean add(T e)
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public boolean remove(Object o)
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public boolean containsAll(Collection<?> c)
    {
        return list.containsAll(c);
    }

    public boolean addAll(Collection<? extends T> c)
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public boolean addAll(int index,Collection<? extends T> c)
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public boolean removeAll(Collection<?> c)
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public boolean retainAll(Collection<?> c)
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public void clear()
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public T get(int index)
    {
        return list.get(index);
    }

    public T set(int index,T element)
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public void add(int index,T element)
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public T remove(int index)
    {
        throw new IllegalStateException("List cannot be changed");
    }

    public int indexOf(Object o)
    {
        return list.indexOf(o);
    }

    public int lastIndexOf(Object o)
    {
        return list.lastIndexOf(o);
    }

    public ListIterator<T> listIterator()
    {
        return list.listIterator();
    }

    public ListIterator<T> listIterator(int index)
    {
        return list.listIterator(index);
    }

    public List<T> subList(int fromIndex,int toIndex)
    {
        throw new IllegalStateException("Invalid sublist request");
    }

    public T getFirst()
    {
        if(isEmpty())
            return null;
        else
            return get(0);
    }

    public T getLast()
    {
        if(isEmpty())
            return null;
        else
            return get(size()-1);
    }
}
