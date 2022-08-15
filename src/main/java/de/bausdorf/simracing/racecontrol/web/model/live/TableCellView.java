package de.bausdorf.simracing.racecontrol.web.model.live;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 bausdorf engineering
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TableCellView {
	private String value;
	private CssClassType displayType;
	private String cssClassString;

	public static TableCellViewBuilder builder() {
		return new TableCellViewBuilder();
	}

	public void setDisplayType(CssClassType classType) {
		this.displayType = classType;
		this.cssClassString = classType.getClassString();
	}

	public String getValue() {
		return this.value;
	}

	public CssClassType getDisplayType() {
		return this.displayType;
	}

	public String getCssClassString() {
		return this.cssClassString;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof TableCellView)) {
			return false;
		}
		final TableCellView other = (TableCellView) o;
		if (!other.canEqual(this)) {
			return false;
		}
		final Object this$value = this.getValue();
		final Object other$value = other.getValue();
		if (this$value == null ? other$value != null : !this$value.equals(other$value)) {
			return false;
		}
		final Object this$displayType = this.getDisplayType();
		final Object other$displayType = other.getDisplayType();
		if (this$displayType == null ? other$displayType != null : !this$displayType.equals(other$displayType)) {
			return false;
		}
		final Object this$cssClassString = this.getCssClassString();
		final Object other$cssClassString = other.getCssClassString();
		if (this$cssClassString == null ? other$cssClassString != null : !this$cssClassString.equals(other$cssClassString)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof TableCellView;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $value = this.getValue();
		result = result * PRIME + ($value == null ? 43 : $value.hashCode());
		final Object $displayType = this.getDisplayType();
		result = result * PRIME + ($displayType == null ? 43 : $displayType.hashCode());
		final Object $cssClassString = this.getCssClassString();
		result = result * PRIME + ($cssClassString == null ? 43 : $cssClassString.hashCode());
		return result;
	}

	public String toString() {
		return "TableCellView(value=" + this.getValue() + ", displayType=" + this.getDisplayType() + ", cssClassString="
				+ this.getCssClassString() + ")";
	}

	public static class TableCellViewBuilder {

		private String value;
		private CssClassType displayType;
		private String cssClassString;

		TableCellViewBuilder() {
		}

		public TableCellViewBuilder value(String value) {
			this.value = value;
			return this;
		}

		public TableCellViewBuilder displayType(CssClassType displayType) {
			this.displayType = displayType;
			this.cssClassString = displayType.getClassString();
			return this;
		}

		public TableCellView build() {
			return new TableCellView(value, displayType, cssClassString);
		}

		public String toString() {
			return "TableCellView.TableCellViewBuilder(value=" + this.value + ", displayType=" + this.displayType + ", cssClassString="
					+ this.cssClassString + ")";
		}
	}
}
