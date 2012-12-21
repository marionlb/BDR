/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.Attribute;
import ca.uqac.dim.turtledb.ConditionVisitor;
import ca.uqac.dim.turtledb.Equality;
import ca.uqac.dim.turtledb.LogicalAnd;
import ca.uqac.dim.turtledb.LogicalOr;

/**
 *
 * @author fx
 */
public class AttributeVerificatorConditionVisitor extends ConditionVisitor
{

	private String tableName;
	private boolean tableFound;

	public boolean isTableFound()
	{
		return tableFound;
	}
			
	public AttributeVerificatorConditionVisitor(String tableName)
	{
		this.tableName = tableName;
	}

	
	@Override
	public void visit(LogicalAnd c)
	{
	}

	@Override
	public void visit(LogicalOr c)
	{
	}

	@Override
	public void visit(Equality c)
	{
		if (c.getLeft() instanceof Attribute)
		{
			Attribute a = (Attribute) c.getLeft();
			if (a.getTableName().equals(tableName) )
			{
				tableFound = true;
			}
		}	
		if (c.getRight() instanceof Attribute)
		{
			Attribute a = (Attribute) c.getRight();
			if (a.getTableName().equals(tableName) )
			{
				tableFound = true;
			}
		}	
	}
	
}
