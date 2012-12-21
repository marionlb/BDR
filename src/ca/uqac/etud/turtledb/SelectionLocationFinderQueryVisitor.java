/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.Condition;
import ca.uqac.dim.turtledb.Intersection;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.NAryRelation;
import ca.uqac.dim.turtledb.Product;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.QueryVisitor;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Selection;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.Union;
import ca.uqac.dim.turtledb.VariableTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Trouve les endroits où placer de manière optimale les selections
 * @author fx
 */
public class SelectionLocationFinderQueryVisitor extends QueryVisitor
{

	private Condition condition;
	private Map<Relation, List<Relation>> selectPos = new HashMap<Relation, List<Relation>>();

	public Map<Relation, List<Relation>> getSelectPos()
	{
		return selectPos;
	}

	public SelectionLocationFinderQueryVisitor(Condition cond)
	{
		this.condition = cond;

	}


	@Override
	public void visit(Projection prjctn) throws VisitorException
	{
		if (VerifyCondition(prjctn.getRelation()))
		{
			selectPos.put(prjctn, null);
		}
	}

	@Override
	public void visit(Selection slctn) throws VisitorException
	{
		if (VerifyCondition(slctn.getRelation()))
		{
			selectPos.put(slctn, null);
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
		NAryvisit(union);
	}

	@Override
	public void visit(Intersection i) throws VisitorException
	{
		NAryvisit(i);
	}

	@Override
	public void visit(Join join) throws VisitorException
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
			selectPos.put(join, concernedList);
		}

	}

	@Override
	public void visit(Product prdct) throws VisitorException
	{
		NAryvisit(prdct);
	}

	private void NAryvisit(NAryRelation rel)
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
			selectPos.put(rel, concernedList);
		}
	}
	private boolean VerifyCondition(Relation r)
	{
		if (r instanceof VariableTable)
		{
			VariableTable vt = (VariableTable) r;
			AttributeVerificatorConditionVisitor avcv = new AttributeVerificatorConditionVisitor(vt.getName());
			condition.accept(avcv);
			return avcv.isTableFound();
		}
		return false;

	}
}
