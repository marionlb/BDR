/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.uqac.dim.turtledb.Attribute;
import ca.uqac.dim.turtledb.Intersection;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.MQueryVisitor;
import ca.uqac.dim.turtledb.NAryRelation;
import ca.uqac.dim.turtledb.Product;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Schema;
import ca.uqac.dim.turtledb.Selection;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.Union;
import ca.uqac.dim.turtledb.VariableTable;

/**
 *
 * Trouve les endroits où placer de manière optimale les selections
 *
 * @author fx
 */
public class ProjectionLocationFinderMQueryVisitor extends MQueryVisitor
{

	private Schema sch;
	private Map<Relation, List<Relation>> projPos = new HashMap<Relation, List<Relation>>();

	public Map<Relation, List<Relation>> getProjPos()
	{
		return projPos;
	}

	public ProjectionLocationFinderMQueryVisitor(Schema cond)
	{
		this.sch = cond;

	}

	@Override
	public void visit(Projection prjctn) throws MVisitorException
	{
		if (VerifyCondition(prjctn.getRelation()))
		{
			projPos.put(prjctn, null);
		} else
		{
			prjctn.getRelation().maccept(this);
		}
	}

	@Override
	public void visit(Selection slctn) throws MVisitorException
	{
		if (VerifyCondition(slctn.getRelation()))
		{
			projPos.put(slctn, null);
		} else
		{
			slctn.getRelation().maccept(this);
		}

	}

	@Override
	public void visit(Table table) throws MVisitorException
	{
	}

	@Override
	public void visit(VariableTable vt) throws MVisitorException
	{
	}

	@Override
	public void visit(Union union) throws MVisitorException
	{
		NAryvisit(union);
	}

	@Override
	public void visit(Intersection i) throws MVisitorException
	{
		NAryvisit(i);
	}

	@Override
	public void visit(Join join) throws MVisitorException
	{
		List<Relation> concernedList = new ArrayList<Relation>();

		if (VerifyCondition(join.getLeft()))
		{
			concernedList.add(join.getLeft());
		} else if (VerifyCondition(join.getRight()))
		{
			concernedList.add(join.getRight());
		}
		if (!concernedList.isEmpty())
		{
			projPos.put(join, concernedList);
		}
		else
		{
	    join.getLeft().maccept(this);
		join.getRight().maccept(this);
		}

	}

	@Override
	public void visit(Product prdct) throws MVisitorException
	{
		NAryvisit(prdct);
	}

	private void NAryvisit(NAryRelation rel) throws MVisitorException
	{
		List<Relation> concernedList = new ArrayList<Relation>();

		for (Relation r : rel.getRelations())
		{
			if (VerifyCondition(r))
			{
				concernedList.add(r);
			}
		}
		if (!concernedList.isEmpty())
		{
			projPos.put(rel, concernedList);
		}
		else
		{
		for (Relation c : rel.getRelations())
			c.maccept(this);
		}
	}

	private boolean VerifyCondition(Relation r)
	{
		if (r instanceof VariableTable)
		{
			VariableTable vt = (VariableTable) r;
			for (Attribute a : sch)
			{
				if (a.getTableName().equalsIgnoreCase(vt.getName()))
				{
					return true;
				}
			}
		}
		// Il manque une interface "Conditional" pour factoriser Join et Selection
		if (r instanceof Join)
		{
			Join rj = (Join) r;
			boolean isConcerned = false;
			for (Attribute a : sch)
			{
				AttributeVerificatorConditionVisitor avcv = new AttributeVerificatorConditionVisitor(a.getTableName());
				rj.getCondition().accept(avcv);
				isConcerned = avcv.isTableFound();
				if (isConcerned)
				{
					break;
				}
			}
			if (isConcerned)
			{
				return true;
			}
		}
		if (r instanceof Selection)
		{
			Selection rj = (Selection) r;
			boolean isConcerned = false;
			for (Attribute a : sch)
			{
				AttributeVerificatorConditionVisitor avcv = new AttributeVerificatorConditionVisitor(a.getTableName());
				rj.getCondition().accept(avcv);
				isConcerned = avcv.isTableFound();
				if (isConcerned)
				{
					break;
				}
			}
			if (isConcerned)
			{
				return true;
			}
		}
		
		return false;

	}
}
