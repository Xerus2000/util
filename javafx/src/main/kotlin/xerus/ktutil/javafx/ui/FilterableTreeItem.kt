/** Initial implementation by Christoph Keimel from [http://www.kware.net/?p=204#The_Filterable_Tree_Item].
 * Adapted & Modified by Janek Fischer. */
package xerus.ktutil.javafx.ui

import javafx.beans.Observable
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.scene.Node
import javafx.scene.control.CheckBoxTreeItem
import javafx.scene.control.TreeItem
import javafx.scene.control.cell.CheckBoxTreeCell
import xerus.ktutil.javafx.properties.bind
import xerus.ktutil.javafx.properties.dependOn
import xerus.ktutil.nullIfEmpty
import java.util.function.Predicate

/**
 * An extension of [TreeItem] with the possibility to filter its children. To enable filtering it is necessary to
 * set the [TreeItemPredicate]. If a predicate is set, then the tree item will also use this predicate to filter
 * its children (if they are of the type FilterableTreeItem).<br></br>
 *
 *
 * A tree item that has children will not be filtered. The predicate will only be evaluated if the TreeItem is a leaf.
 * Since the predicate is also set for the child tree items, the tree item in question can turn into a leaf if all of
 * its children are filtered out and [autoLeaf] is set to true.
 *
 * This class extends [CheckBoxTreeItem] so it can, but does not need to be, used in conjunction with
 * [CheckBoxTreeCell] cells.
 *
 * @param T The type of the [value] property within this [TreeItem].
 * @param value The object to be stored as the value of this CheckBoxTreeItem.
 * @param graphic The Node to show in the TreeView next to this CheckBoxTreeItem.
 * @param selected The initial value of the [selectedProperty].
 * @param independent The initial value of the [independentProperty].
 */
@Suppress("unused")
class FilterableTreeItem<T>(value: T, graphic: Node? = null, selected: Boolean = false, independent: Boolean = false) :
	CheckBoxTreeItem<T>(value, graphic, selected, independent) {
	
	/** @return the list of children that is backing the filtered list. */
	val internalChildren: ObservableList<TreeItem<T>> = FXCollections.observableArrayList<TreeItem<T>>()
	
	private val predicate = SimpleObjectProperty<TreeItemPredicate<T>?>()
	
	init {
		val filteredList = FilteredList<TreeItem<T>>(this.internalChildren)
		filteredList.predicateProperty().dependOn(this.predicate) { predicate ->
			Predicate { child: TreeItem<T> ->
				val result = predicate?.invoke(this, child.value) ?: true
				// Set the predicate of child items for recursive filtering
				val filterableChild = (child as? FilterableTreeItem<T>)
				filterableChild?.setPredicate(if(keepAllChildren && result) null else predicate)
				// If there is no predicate, keep this tree item
				if(predicate == null) {
					if(autoExpand)
						child.isExpanded = false
					return@Predicate true
				}
				// If there are children, keep this tree item
				if(child.children.size > 0) {
					if(autoExpand)
						child.isExpanded = true
					return@Predicate true
				}
				// If autoLeaf is off and this item has filterable children that are all filtered out, then hide this
				if(!autoLeaf && filterableChild != null && filterableChild.internalChildren.size > 0)
					return@Predicate false
				// This is a leaf, only filtered by the Predicate
				result
			}
		}
		
		setHiddenFieldChildren(filteredList)
	}
	
	/**
	 * Set the hidden private field [TreeItem.children] through reflection and hook the hidden
	 * [ListChangeListener] in [TreeItem.childrenListener] to the list
	 * @param list the list to set
	 */
	private fun setHiddenFieldChildren(list: ObservableList<TreeItem<T>>) {
		try {
			val childrenField = TreeItem::class.java.getDeclaredField("children")
			childrenField.isAccessible = true
			childrenField.set(this, list)
			
			val declaredField = TreeItem::class.java.getDeclaredField("childrenListener")
			declaredField.isAccessible = true
			@Suppress("UNCHECKED_CAST")
			list.addListener(declaredField.get(this) as ListChangeListener<TreeItem<T>>)
		} catch(e: Exception) {
			e.printStackTrace()
		}
		
	}
	
	fun predicateProperty() = this.predicate
	
	fun getPredicate() = this.predicate.get()
	
	fun setPredicate(predicate: TreeItemPredicate<T>?) = this.predicate.set(predicate)
	
	/** Automatically update the predicate */
	fun bindPredicate(filter: (T) -> Boolean, vararg dependencies: Observable) {
		predicate.bind({ { _, value -> filter.invoke(value) } }, *dependencies)
	}
	
	/** Establishes a Binding to that Property that defaults to filtering the value using the string, ignoring case
	 * if the current String of the Property is empty, the Predicate is automatically set to null */
	fun bindPredicate(property: Property<String>, function: (T, String) -> Boolean = { value, text -> value.toString().contains(text, true) }) {
		predicate.bind({ property.value.nullIfEmpty()?.let { text -> { _, value -> function(value, text) } } }, property)
	}
	
	override fun toString(): String = "FilterableTreeItem(value=$value, predicate=$predicate)"
	
	companion object {
		/** when true, if no child of an item matches the [predicate], it will turn into a leaf */
		var autoLeaf = true
		/** when true, children of items that match the [predicate] will automatically be kept */
		var keepAllChildren = true
		/** when true, all items will collapse when the [predicate] is null and expand when it is not */
		var autoExpand = false
	}
	
}

typealias TreeItemPredicate<T> = (TreeItem<T>, T) -> Boolean
