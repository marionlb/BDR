/*-------------------------------------------------------------------------
    Simple distributed database engine
    Copyright (C) 2012  Sylvain Hallé

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 -------------------------------------------------------------------------*/
package ca.uqac.dim.turtledb;

public abstract class UnaryRelation extends Relation implements Cloneable
{
  protected Relation m_relation;
  protected boolean toTrash = false;

	public boolean isToTrash()
	{
		return toTrash;
	}

	public void setToTrash(boolean toTrash)
	{
		this.toTrash = toTrash;
	}
  
  public void setRelation(Relation r)
  {
    m_relation = r;
  }

  
  public int tupleCount()
  {
    return m_relation.tupleCount();
  }
  
  protected abstract class UnaryRelationStreamIterator extends RelationStreamIterator
  {
    protected RelationIterator m_childIterator;
    
    public UnaryRelationStreamIterator()
    {
      super();
      m_childIterator = m_relation.streamIterator();
    }
    
    @Override
    public void reset()
    {
      super.reset();
      m_childIterator.reset();
    }
  }
  
  protected abstract class UnaryRelationCacheIterator extends RelationCacheIterator
  {
    protected void getIntermediateResult()
    {
      Table tab_out = new Table(m_relation.getSchema());
      RelationIterator it = m_relation.cacheIterator();
      while (it.hasNext())
      {
        Tuple t = it.next();
        tab_out.put(t);
      }
      m_intermediateResult = tab_out;
    }
  }
  
  ///////////temporaire//////////////
  public String toString() {
	  String res="", sep="", cond="";
	  if(this instanceof Projection) {
		  sep = "Proj";
		  cond = ((Projection)this).m_schema.toString() +" ";
	  } else if (this instanceof Selection) {
		  sep = "Sel";
		  cond = "[ "+ ((Selection)this).m_condition.toString()+ " ]";
	  } else {
		  sep = "Z";
	  }
	 
	  res = sep + cond + "("+ m_relation + ") ";
	  return res;
  }
  
  public Relation getRelation() {
	  return m_relation;
  }
	@Override
	public Object clone() {
		UnaryRelation r = null;

		r = (UnaryRelation) super.clone();
		if(this.m_relation != null)
			r.m_relation = (Relation) this.m_relation.clone();

		// on renvoie le clone
		return r;
	}
}
