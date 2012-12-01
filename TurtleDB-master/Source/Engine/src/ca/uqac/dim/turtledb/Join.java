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

import ca.uqac.dim.turtledb.QueryVisitor.VisitorException;

public class Join extends BinaryRelation
{
  protected Product m_product;
  protected Condition m_condition;
  
  public Join()
  {
    super();
    m_product = new Product();
  }
  
  public Join(Condition c)
  {
    this();
    m_condition = c;
  }
  
  public void setCondition(Condition c)
  {
    m_condition = c;
  }
  
  @Override
  public Schema getSchema()
  {
    return m_product.getSchema();
  }
  
  public void addOperand(Relation r)
  {
    m_product.addOperand(r);
  }
  
  public int tupleCount()
  {
    return m_product.tupleCount();
  }
  

  
  @Override
  public void accept(QueryVisitor v) throws VisitorException
  {
    super.acceptBinary(v);
    v.visit(this);
  }
  
  protected class JoinStreamIterator extends BinaryRelationStreamIterator
  {
    protected RelationStreamIterator m_childIterator;
    
    public JoinStreamIterator()
    {
      super();
      m_childIterator = m_product.streamIterator();
      reset();
    }
    
    
    /**
     * Implementation of internalNext.
     */
    @Override
    protected Tuple internalNext()
    {
      while (m_childIterator.hasNext())
      {
        Tuple t = m_childIterator.next();
        if (m_condition.evaluate(t))
          return t;
      }
      return null;
    }
    
    public void reset()
    {
      super.reset();
      m_childIterator.reset();
    }
  }

  @Override
  public RelationIterator streamIterator()
  {
    return new JoinStreamIterator();
  }

  @Override
  public RelationIterator cacheIterator()
  {
    return new JoinCacheIterator();
  }
  
  protected class JoinCacheIterator extends RelationCacheIterator
  {
    public JoinCacheIterator()
    {
      super();
      m_intermediateResult = null;
    }
    
    protected void getIntermediateResult()
    {
      m_intermediateResult = new Table(m_product.getSchema());
      RelationIterator it = m_product.cacheIterator();
      while (it.hasNext())
      {
        Tuple t = it.next();
        if (m_condition.evaluate(t))
          m_intermediateResult.put(t);
      }
    }
    
  }

}
