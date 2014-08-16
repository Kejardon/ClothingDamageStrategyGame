/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package JExtensions;

import java.awt.Container;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.JComponent;

/**
 *
 * @author Kevin
 */
public abstract class EnlargingComponent extends JComponent
{
	public boolean isPreferredSizeSet()
	{
		if(super.isPreferredSizeSet())
			return true;
		return (getParent()!=null);
	}
	public Dimension getPreferredSize()
	{
		if(super.isPreferredSizeSet())
			return super.getPreferredSize();
		Container parent=getParent();
		Dimension parentDimension=parent.getSize();
		return getPreferredSize(parentDimension);
	}
	protected abstract Dimension getPreferredSize(Dimension parentDimension);
	
	public static abstract class EnlargingBox extends Box
	{
		public EnlargingBox(int i){super(i);}
		public boolean isPreferredSizeSet()
		{
			if(super.isPreferredSizeSet())
				return true;
			return (getParent()!=null);
		}
		public Dimension getPreferredSize()
		{
			if(super.isPreferredSizeSet())
				return super.getPreferredSize();
			Container parent=getParent();
			Dimension parentDimension=parent.getSize();
			return getPreferredSize(parentDimension);
		}
		protected abstract Dimension getPreferredSize(Dimension parentDimension);
	}
}
