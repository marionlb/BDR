/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.MQueryVisitor;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
			 * @author fx
 */
public class EmptyMQueryVisitor extends MQueryVisitor
{

	@Override
	public void visit(Projection r) throws MVisitorException
	{
		r.getRelation().maccept(this);
	}

	@Override
	public void visit(Selection r) throws MVisitorException
	{
		r.getRelation().maccept(this);
	}

	@Override
	public void visit(Table r) throws MVisitorException
	{
	}

	@Override
	public void visit(VariableTable r) throws MVisitorException
	{
    if (r.getRelation() != null)
      r.getRelation().maccept(this);
	}

	@Override
	public void visit(Union r) throws MVisitorException
	{
		for (Relation c : r.getRelations())
			c.maccept(this);
	}

	@Override
	public void visit(Intersection r) throws MVisitorException
	{
		for (Relation c : r.getRelations())
			c.maccept(this);
	}

	@Override
	public void visit(Join r) throws MVisitorException
	{
	    r.getLeft().maccept(this);
		r.getRight().maccept(this);
	}

	@Override
	public void visit(Product r) throws MVisitorException
	{
		for (Relation c : r.getRelations())
			c.maccept(this);
	}

}
