/* This file was automatically generated by TightDB. */

package com.tightdb.generated;


import com.tightdb.*;
import com.tightdb.lib.*;

/**
 * This class represents a TightDB view and was automatically generated.
 */
public class PhoneView extends AbstractView<Phone, PhoneView, PhoneQuery> {

	public final StringRowsetColumn<Phone, PhoneView, PhoneQuery> type = new StringRowsetColumn<Phone, PhoneView, PhoneQuery>(PhoneTable.TYPES, rowset, 0, "type");
	public final StringRowsetColumn<Phone, PhoneView, PhoneQuery> number = new StringRowsetColumn<Phone, PhoneView, PhoneQuery>(PhoneTable.TYPES, rowset, 1, "number");

	public PhoneView(TableViewBase viewBase) {
		super(PhoneTable.TYPES, viewBase);
	}

}