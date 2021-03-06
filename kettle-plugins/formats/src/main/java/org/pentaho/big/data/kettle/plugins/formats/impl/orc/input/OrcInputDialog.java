/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.pentaho.big.data.kettle.plugins.formats.impl.orc.input;

import java.util.ArrayList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.pentaho.big.data.kettle.plugins.formats.FormatInputOutputField;
import org.pentaho.big.data.kettle.plugins.formats.impl.orc.BaseOrcStepDialog;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.hadoop.shim.api.format.SchemaDescription;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;

public class OrcInputDialog extends BaseOrcStepDialog<OrcInputMeta> {

  private static final int SHELL_WIDTH = 526;
  private static final int SHELL_HEIGHT = 506;

  private static final int ORC_PATH_COLUMN_INDEX = 1;

  private static final int FIELD_NAME_COLUMN_INDEX = 2;

  private static final int FIELD_TYPE_COLUMN_INDEX = 3;

  private static final int FIELD_SOURCE_TYPE_COLUMN_INDEX = 4;

  private TableView wInputFields;
  protected TextVar wSchemaPath;
  protected Button wbSchemaBrowse;

  private Button wPassThruFields;

  public OrcInputDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (OrcInputMeta) in, transMeta, sname );
  }

  @Override
  protected void createUI( ) {
    Control prev = createHeader();

    //main fields
    prev = addFileWidgets( prev );

    createFooter( shell );

    Label separator = new Label( shell, SWT.HORIZONTAL | SWT.SEPARATOR );
    FormData fdSpacer = new FormData();
    fdSpacer.height = 2;
    fdSpacer.left = new FormAttachment( 0, 0 );
    fdSpacer.bottom = new FormAttachment( wCancel, -MARGIN );
    fdSpacer.right = new FormAttachment( 100, 0 );
    separator.setLayoutData( fdSpacer );

    Group fieldsContainer = new Group( shell, SWT.SHADOW_IN );
    fieldsContainer.setLayout( new FormLayout() );
    fieldsContainer.setText( BaseMessages.getString( PKG, "OrcInputDialog.Fields.Label" ) );
    new FD( fieldsContainer ).left( 0, 0 ).top( prev, MARGIN ).right( 100, 0 ).bottom( separator, -MARGIN ).apply();

    // Accept fields from previous steps?
    //
    wPassThruFields = new Button( fieldsContainer, SWT.CHECK );
    wPassThruFields.setText( BaseMessages.getString( PKG, "OrcInputDialog.PassThruFields.Label" ) );
    wPassThruFields.setToolTipText( BaseMessages.getString( PKG, "OrcInputDialog.PassThruFields.Tooltip" ) );
    wPassThruFields.setOrientation( SWT.LEFT_TO_RIGHT );
    props.setLook( wPassThruFields );
    new FD( wPassThruFields ).left( 0, MARGIN ).top( 0, MARGIN ).apply();

    //get fields button
    lsGet = new Listener() {
      public void handleEvent( Event e ) {
        populateFieldsTable();
      }
    };
    Button wGetFields = new Button( fieldsContainer, SWT.PUSH );
    wGetFields.setText( BaseMessages.getString( PKG, "OrcInputDialog.Fields.Get" ) );
    props.setLook( wGetFields );
    new FD( wGetFields ).bottom( 100, -FIELDS_SEP ).right( 100, -MARGIN ).apply();
    wGetFields.addListener( SWT.Selection, lsGet );

    // fields table
    ColumnInfo[] parameterColumns = new ColumnInfo[] {
      new ColumnInfo( BaseMessages.getString( PKG, "OrcInputDialog.Fields.column.Path" ),
        ColumnInfo.COLUMN_TYPE_TEXT, false, true ),
      new ColumnInfo( BaseMessages.getString( PKG, "OrcInputDialog.Fields.column.Name" ),
        ColumnInfo.COLUMN_TYPE_TEXT, false, false ),
      new ColumnInfo( BaseMessages.getString( PKG, "OrcInputDialog.Fields.column.Type" ),
        ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMetaFactory.getValueMetaNames() ),
      new ColumnInfo( BaseMessages.getString( PKG, "OrcInputDialog.Fields.column.SourceType" ),
         ColumnInfo.COLUMN_TYPE_TEXT, ValueMetaFactory.getValueMetaNames(), true ) };
    parameterColumns[0].setAutoResize( false );
    parameterColumns[1].setUsingVariables( true );
    parameterColumns[3].setAutoResize( false );

    wInputFields =
            new TableView( transMeta, fieldsContainer, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER | SWT.NO_SCROLL | SWT.V_SCROLL,
                    parameterColumns, 7, null, props );
    ColumnsResizer resizer = new ColumnsResizer( 0, 50, 25, 25, 0 );
    wInputFields.getTable().addListener( SWT.Resize, resizer );

    props.setLook( wInputFields );
    new FD( wInputFields ).left( 0, MARGIN ).right( 100, -MARGIN ).top( wPassThruFields, FIELDS_SEP )
            .bottom( wGetFields, -FIELDS_SEP ).apply();

    wInputFields.setRowNums();
    wInputFields.optWidth( true );

    for ( ColumnInfo col : parameterColumns ) {
      col.setAutoResize( false );
    }
    resizer.addColumnResizeListeners( wInputFields.getTable() );
    setTruncatedColumn( wInputFields.getTable(), 1 );
    if ( !Const.isWindows() ) {
      addColumnTooltip( wInputFields.getTable(), 1 );
    }
  }

  protected void populateFieldsTable() {
    String orcFileName = wPath.getText();
    orcFileName = transMeta.environmentSubstitute( orcFileName );
    try {
      SchemaDescription schemaDescription = OrcInput.retrieveSchema( meta.getNamedClusterServiceLocator(), meta.getNamedCluster(), orcFileName );
      wInputFields.clearAll();
      for ( SchemaDescription.Field field : schemaDescription ) {
        TableItem item = new TableItem( wInputFields.table, SWT.NONE );
        if ( field != null ) {
          setField( item, field.formatFieldName, 1 );
          setField( item, field.pentahoFieldName, 2 );
          setField( item, ValueMetaFactory.getValueMetaName( field.pentahoValueMetaType ), 3 );
          setField( item, ValueMetaFactory.getValueMetaName( field.pentahoValueMetaType ), 4 );
        }
      }

      wInputFields.removeEmptyRows();
      wInputFields.setRowNums();
      wInputFields.optWidth( true );
    } catch ( Exception ex ) {
      logError( BaseMessages.getString( PKG, "OrcInput.Error.UnableToLoadSchemaFromContainerFile" ), ex );
      new ErrorDialog( shell, stepname, BaseMessages.getString( PKG,
        "OrcInput.Error.UnableToLoadSchemaFromContainerFile", orcFileName ), ex );
    }
  }

  private void setField( TableItem item, String fieldValue, int fieldIndex ) {
    if ( !Utils.isEmpty( fieldValue ) ) {
      item.setText( fieldIndex, fieldValue );
    }
  }

  /**
   * Read the data from the meta object and show it in this dialog.
   */
  @Override
  protected void getData( OrcInputMeta meta ) {
    if ( meta.getFilename() != null && meta.getFilename().length() > 0 ) {
      wPath.setText( meta.getFilename() );
    }
    wPassThruFields.setSelection( meta.inputFiles.passingThruFields );
    int itemIndex = 0;
    for ( FormatInputOutputField inputField : meta.getInputFields() ) {
      TableItem item = null;
      if ( itemIndex < wInputFields.table.getItemCount() ) {
        item = wInputFields.table.getItem( itemIndex );
      } else {
        item = new TableItem( wInputFields.table, SWT.NONE );
      }

      if ( inputField.getPath() != null ) {
        item.setText( ORC_PATH_COLUMN_INDEX, inputField.getPath() );
      }
      if ( inputField.getName() != null ) {
        item.setText( FIELD_NAME_COLUMN_INDEX, inputField.getName() );
      }
      if ( inputField.getTypeDesc() != null ) {
        item.setText( FIELD_TYPE_COLUMN_INDEX, inputField.getTypeDesc() );
      }
      if ( inputField.getSourceTypeDesc() != null ) {
        item.setText( FIELD_SOURCE_TYPE_COLUMN_INDEX, inputField.getSourceTypeDesc() );
      }
      itemIndex++;
    }
  }

  /**
   * Fill meta object from UI options.
   */
  @Override
  protected void getInfo( OrcInputMeta meta, boolean preview ) {
    String filePath = wPath.getText();
    if ( filePath != null && !filePath.isEmpty() ) {
      meta.allocateFiles( 1 );
      meta.setFilename( wPath.getText().trim() );
    }

    meta.inputFiles.passingThruFields = wPassThruFields.getSelection();

    int nrFields = wInputFields.nrNonEmpty();
    meta.setInputFields( new ArrayList<>() );
    for ( int i = 0; i < nrFields; i++ ) {
      TableItem item = wInputFields.getNonEmpty( i );
      FormatInputOutputField field = new FormatInputOutputField();
      field.setPath( item.getText( ORC_PATH_COLUMN_INDEX ) );
      field.setName( item.getText( FIELD_NAME_COLUMN_INDEX ) );
      field.setType( ValueMetaFactory.getIdForValueMeta( item.getText( FIELD_TYPE_COLUMN_INDEX ) ) );
      meta.getInputFields().add( field );
    }
  }

  private void doPreview() {
    getInfo( meta, true );
    TransMeta previewMeta =
      TransPreviewFactory.generatePreviewTransformation( transMeta, meta, wStepname.getText() );
    transMeta.getVariable( "Internal.Transformation.Filename.Directory" );
    previewMeta.getVariable( "Internal.Transformation.Filename.Directory" );

    EnterNumberDialog numberDialog =
      new EnterNumberDialog( shell, props.getDefaultPreviewSize(), BaseMessages.getString( PKG,
        "OrcInputDialog.PreviewSize.DialogTitle" ), BaseMessages.getString( PKG,
        "OrcInputDialog.PreviewSize.DialogMessage" ) );
    int previewSize = numberDialog.open();

    if ( previewSize > 0 ) {
      TransPreviewProgressDialog progressDialog =
        new TransPreviewProgressDialog( shell, previewMeta, new String[] { wStepname.getText() },
          new int[] { previewSize } );
      progressDialog.open();

      Trans trans = progressDialog.getTrans();
      String loggingText = progressDialog.getLoggingText();

      if ( !progressDialog.isCancelled() ) {
        if ( trans.getResult() != null && trans.getResult().getNrErrors() > 0 ) {
          EnterTextDialog etd =
            new EnterTextDialog( shell, BaseMessages.getString( PKG, "System.Dialog.PreviewError.Title" ),
              BaseMessages.getString( PKG, "System.Dialog.PreviewError.Message" ), loggingText, true );
          etd.setReadOnly();
          etd.open();
        }
      }

      PreviewRowsDialog prd =
        new PreviewRowsDialog( shell, transMeta, SWT.NONE, wStepname.getText(), progressDialog
          .getPreviewRowsMeta( wStepname.getText() ),
          progressDialog.getPreviewRows( wStepname.getText() ), loggingText );
      prd.open();
    }
  }

  @Override
  protected int getWidth() {
    return SHELL_WIDTH;
  }

  @Override
  protected int getHeight() {
    return SHELL_HEIGHT;
  }

  @Override
  protected String getStepTitle() {
    return BaseMessages.getString( PKG, "OrcInputDialog.Shell.Title" );
  }

  @Override
  protected Listener getPreview() {
    return new Listener() {
      public void handleEvent( Event e ) {
        doPreview();
      }
    };
  }
}
