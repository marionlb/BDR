/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import java.util.List;

import ca.uqac.dim.turtledb.Intersection;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.NAryRelation;
import ca.uqac.dim.turtledb.Product;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.QueryVisitor;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Selection;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.UnaryRelation;
import ca.uqac.dim.turtledb.Union;
import ca.uqac.dim.turtledb.VariableTable;

/**
 *
 * @author fx
 * 
 * Remove useless Selection after optimization
 */
public class CleanerQueryVisitor extends QueryVisitor
{

	@Override
	public void visit(Projection r) throws VisitorException
	{
		if (r.getRelation() instanceof UnaryRelation)
		{
			UnaryRelation s = (UnaryRelation) r.getRelation();
			if (s.isToTrash())
			{
				r.setRelation(s.getRelation());
			}
		}
	}

	@Override
	public void visit(Selection r) throws VisitorException
	{
		if (r.getRelation() instanceof UnaryRelation)
		{
			UnaryRelation s = (UnaryRelation) r.getRelation();
			if (s.isToTrash())
			{
				r.setRelation(s.getRelation());
			}
		}
	}

	@Override
	public void visit(Table r) throws VisitorException
	{
	}

	@Override
	public void visit(VariableTable r) throws VisitorException
	{
	}

	@Override
	public void visit(Union r) throws VisitorException
	{
		NAryVisit(r);
	}

	@Override
	public void visit(Intersection r) throws VisitorException
	{
		NAryVisit(r);
	}

	@Override
	public void visit(Join r) throws VisitorException
	{
		if (r.getLeft() instanceof UnaryRelation)
		{
			UnaryRelation s = (UnaryRelation) r.getLeft();
			if (s.isToTrash())
			{
				r.setLeft(s.getRelation());
			}
		}
		if (r.getRight() instanceof UnaryRelation)
		{
			UnaryRelation s = (UnaryRelation) r.getRight();
			if (s.isToTrash())
			{
				r.setRight(s.getRelation());
			}
		}
	}

	@Override
	public void visit(Product r) throws VisitorException
	{
		NAryVisit(r);
	}
	private void NAryVisit (NAryRelation r)
	{
		List<Relation> rels = r.getRelations();

		for (int i = 0; i < rels.size(); i++)
		{
			if (rels.get(i) instanceof UnaryRelation)
			{
				UnaryRelation s = (UnaryRelation) rels.get(i);
				if (s.isToTrash())
				{
					rels.set(i, s.getRelation());
				}
			}
		}
		
	}
}
