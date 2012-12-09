/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.Condition;
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
		for (String curTable : c.joinTables())
		{
			if (curTable.equals(tableName))
			{
				
			}
		}
	}
	
}
