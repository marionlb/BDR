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

import java.util.Iterator;

import ca.uqac.dim.turtledb.QueryVisitor.VisitorException;

public class Product extends NAryRelation
{ 
  @Override
  public Schema getSchema()
  {
    Schema sch = new Schema();
    for (Relation r : m_relations)
    {
      Schema s = r.getSchema();
      sch.addAll(s);
    }
    return sch;
  }
  

  @Override
  public void accept(QueryVisitor v) throws VisitorException
  {
    super.acceptNAry(v);
    v.visit(this);
  }
  
  protected class ProductStreamIterator extends NAryRelationStreamIterator
  {
    @Override
    protected Tuple internalNext()
    { 
      if (m_first)
      {
        m_first = false;
        super.initializeIteration();
      }
      else
      {  
        int len = m_relations.size();
        // Update m_lastTuple by "incrementing" the vector
        for (int i = len - 1; i >= 0; i--)
        {
          RelationIterator r = m_iterators.get(i);
          if (r.hasNext())
          {
            Tuple t = r.next();
            assert t != null;
            m_lastTuple.setElementAt(t, i);
            break;
          }
          else
          {
            if (i == 0)
              return null; // We exhausted the iteration
            r.reset();
            assert r.hasNext();
            Tuple t = r.next();
            assert t != null;
            m_lastTuple.setElementAt(t, i);
          }
        }
      }
      return Tuple.makeTuple(m_lastTuple);
    }
  }

  @Override
  public RelationStreamIterator streamIterator()
  {
    return new ProductStreamIterator();
  }


  @Override
  public RelationIterator cacheIterator()
  {
    return new ProductCacheIterator();
  }
  
  protected class ProductCacheIterator extends NAryRelationCacheIterator
  {

    @Override
    protected void getIntermediateResult()
    {
      Table tab_out = new Table(getSchema());
      super.getIntermediateResult();
      super.initializeIteration();
      boolean in = true;
      while (in)
      {
        int len = m_relations.size();
        // Update m_lastTuple by "incrementing" the vector
        for (int i = len - 1; i >= 0; i--)
        {
          Table tab = m_results.get(i);
          Iterator<Tuple> r = m_iterators.get(i);
          if (r.hasNext())
          {
            Tuple t = r.next();
            assert t != null;
            m_lastTuple.setElementAt(t, i);
            break;
          }
          else
          {
            if (i == 0)
            {
              in = false;
              break;
            }
            r = tab.tupleIterator();
            m_iterators.setElementAt(r, i);
            assert r.hasNext();
            Tuple t = r.next();
            assert t != null;
            m_lastTuple.setElementAt(t, i);
          }
        }
        tab_out.put(Tuple.makeTuple(m_lastTuple));
      }
      m_intermediateResult = tab_out;
    }
    
  }
}
