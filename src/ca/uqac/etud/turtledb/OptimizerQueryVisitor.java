/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.Intersection;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.Product;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.QueryVisitor;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Selection;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.Union;
import ca.uqac.dim.turtledb.VariableTable;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fx
 */
public class OptimizerQueryVisitor extends QueryVisitor
{

	@Override
	public void visit(Projection prjctn) throws VisitorException
	{
	}

	@Override
	public void visit(Selection slctn) throws VisitorException
	{
		SelectionLocationFinderQueryVisitor sqv = new SelectionLocationFinderQueryVisitor(slctn.getCondition());
	slctn.getRelation().accept(sqv);
	Map<Relation, List<Relation>> pos = sqv.getSelectPos();		
	
	for (Relation key : pos.keySet())
	{
		if (key instanceof Projection)
		{
			Projection p = ((Projection) key);
			
			((Projection) key).getRelation();
		}
	}
	
	}

	@Override
	public void visit(Table table) throws VisitorException
	{
	}

	@Override
	public void visit(VariableTable vt) throws VisitorException
	{
	}

	@Override
	public void visit(Union union) throws VisitorException
	{
	}

	@Override
	public void visit(Intersection i) throws VisitorException
	{
	}

	@Override
	public void visit(Join join) throws VisitorException
	{
	}

	@Override
	public void visit(Product prdct) throws VisitorException
	{
	}
	
}
