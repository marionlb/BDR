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

import java.util.*;

public class Projection extends UnaryRelation
{
  protected Schema m_schema;
  
  public Projection(Schema sch, Relation rel)
  {
    super();
    m_schema = sch;
    m_relation = rel;
  }

  @Override
  public Schema getSchema()
  {
    return m_schema;
  }
  
  /**
   * Computes the projection of a tuple over a given schema
   * @param t The original tuple
   * @return The projected tuple
   */
  private Tuple project(Tuple t)
  {
    if (t == null)
      return null;
    Value[] parts = new Value[m_schema.size()];
    int i = 0;
    for (Attribute a : m_schema)
    {
      parts[i++] = t.get(a);
    }
    return new Tuple(m_schema, parts);
  }
  
  public void setSchema(Schema sch)
  {
    assert sch != null;
    m_schema = sch;
  }

  @Override
  public void accept(QueryVisitor v) throws EmptyQueryVisitor.VisitorException 
  {
    m_relation.accept(v);
    v.visit(this);
  }
  
  protected class ProjectionStreamIterator extends UnaryRelationStreamIterator
  {
    public ProjectionStreamIterator()
    {
      super();
      m_outputTuples = new LinkedList<Tuple>();
    }
    
    protected Tuple internalNext()
    {
      Tuple t = m_childIterator.next();
      return project(t);    
    }
  }
  
  protected class ProjectionCacheIterator extends UnaryRelationCacheIterator
  {
    public void getIntermediateResult()
    {
      Table tab_out = new Table(getSchema());
      super.getIntermediateResult();
      Iterator<Tuple> it = m_intermediateResult.tupleIterator();
      while (it.hasNext())
      {
        Tuple t = it.next();
        Tuple t2 = project(t);
        tab_out.put(t2);
      }
      m_intermediateResult = tab_out;
    }
  }

  @Override
  public RelationStreamIterator streamIterator()
  {
    return new ProjectionStreamIterator();
  }

  @Override
  public RelationIterator cacheIterator()
  {
    return new ProjectionCacheIterator();
  }

}
