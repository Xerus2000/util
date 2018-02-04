/******************************************************************************* Copyright (c) 2014 EM-SOFTWARE and
 * others. All rights reserved. This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Christoph Keimel <c.keimel@emsw.de> - initial API and
 * implementation *******************************************************************************/
package xerus.util.javafx;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.CheckBoxTreeCell;

/**
 * An extension of {@link TreeItem} with the possibility to filter its children. To enable filtering it is necessary to
 * set the {@link TreeItemPredicate}. If a predicate is set, then the tree item will also use this predicate to filter
 * its children (if they are of the type FilterableTreeItem).<br>
 * <p>
 * A tree item that has children will not be filtered. The predicate will only be evaluated if the TreeItem is a leaf.
 * Since the predicate is also set for the child tree items, the tree item in question can turn into a leaf if all of
 * its children are filtered out and {@link #autoLeaf} is set to true.
 * <p>
 * This class extends {@link CheckBoxTreeItem} so it can, but does not need to be, used in conjunction with
 * {@link CheckBoxTreeCell} cells.
 * @param <T> The type of the {@link #getValue() value} property within {@link TreeItem}.
 */
public class FilterableTreeItem<T> extends CheckBoxTreeItem<T> {
	
	/** when true, items with children, but all filtered out, will turn into leafs */
	public static boolean autoLeaf = true;
	
	/** when true, subitems of matched items will automatically be kept */
	public static boolean keepSubitems = true;
	
	/** when true, items will collapse when the {@link #predicate} is null and expand when it is not */
	public static boolean autoExpand = false;
	
	final private ObservableList<TreeItem> sourceList;
	
	private ObjectProperty<TreeItemPredicate> predicate = new SimpleObjectProperty<>();
	
	/**
	 * Creates a new {@link TreeItem} with filtered children.
	 * @param value the value of the {@link TreeItem}
	 */
	public FilterableTreeItem(T value) {
		super(value);
		this.sourceList = FXCollections.observableArrayList();
		FilteredList<TreeItem> filteredList = new FilteredList<>(this.sourceList);
		filteredList.predicateProperty().bind(Bindings.createObjectBinding(() -> {
			Predicate<TreeItem> p = child -> {
				// Set the predicate of child items to force filtering
				Boolean result = null;
				FilterableTreeItem filterableChild = null;
				if (child instanceof FilterableTreeItem) {
					filterableChild = (FilterableTreeItem) child;
					if (keepSubitems && (predicate.get() == null || (result = predicate.get().test(this, child.getValue()))))
						filterableChild.setPredicate(null);
					else
						filterableChild.setPredicate(this.predicate.get());
				}
				// If there is no predicate, keep this tree item
				if (this.predicate.get() == null) {
					if (autoExpand)
						child.setExpanded(false);
					return true;
				}
				// If there are children, keep this tree item
				if (child.getChildren().size() > 0) {
					if (autoExpand)
						child.setExpanded(true);
					return true;
				}
				if (!autoLeaf && filterableChild != null && filterableChild.sourceList.size() > 0)
					return false;
				// Otherwise ask the TreeItemPredicate
				if (result == null)
					result = predicate.get().test(this, child.getValue());
				return result;
			};
			return p;
		}, this.predicate));
		
		setHiddenFieldChildren(filteredList);
	}
	
	/**
	 * Set the hidden private field {@link TreeItem#children} through reflection and hook the hidden
	 * {@link ListChangeListener} in {@link TreeItem#childrenListener} to the list
	 * @param list the list to set
	 */
	protected void setHiddenFieldChildren(ObservableList<TreeItem> list) {
		try {
			Field childrenField = TreeItem.class.getDeclaredField("children");
			childrenField.setAccessible(true);
			childrenField.set(this, list);
			
			Field declaredField = TreeItem.class.getDeclaredField("childrenListener");
			declaredField.setAccessible(true);
			list.addListener((ListChangeListener<? super TreeItem>) declaredField.get(this));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the list of children that is backing the filtered list.
	 * @return underlying list of children
	 */
	public ObservableList<TreeItem> getInternalChildren() {
		return this.sourceList;
	}
	
	/** @return the predicate property */
	public final ObjectProperty<TreeItemPredicate> predicateProperty() {
		return this.predicate;
	}
	
	/** @return the predicate */
	public final TreeItemPredicate getPredicate() {
		return this.predicate.get();
	}
	
	/**
	 * Set the predicate
	 * @param predicate the predicate
	 */
	public final void setPredicate(TreeItemPredicate predicate) {
		this.predicate.set(predicate);
	}
	
	@FunctionalInterface
	public interface TreeItemPredicate<T> {
		
		boolean test(TreeItem<T> parent, T value);
		
		static <T> TreeItemPredicate<T> create(Predicate<T> predicate) {
			return (parent, value) -> predicate.test(value);
		}
		
	}
}