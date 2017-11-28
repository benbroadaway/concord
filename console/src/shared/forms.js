import React from "react";
import {Field as RFField} from "redux-form";
import {Checkbox as SUCheckbox, Dropdown as SUDropdown, Form, Input, Label} from "semantic-ui-react";

const CInput = ({input, meta: {error, touched, asyncValidating}, label, required, ...custom}) => {
    const invalid = error && touched;

    return <Form.Field error={invalid} required={required}>

        <label htmlFor={custom.name}>{label}</label>
        <Input id={custom.name} {...input} {...custom} loading={asyncValidating}/>

        {invalid && <Label basic color="red" pointing>{error}</Label>}
    </Form.Field>;
};

const SUComponent = ({widget: Widget, input, meta: {error, touched}, label, required, ...custom}) => {
    const invalid = error && touched;

    return <Form.Field error={invalid} required={required}>

        <label htmlFor={custom.name}>{label}</label>
        <Widget id={custom.name} {...input} {...custom} selection
                onChange={(ev, {value}) => {
                    input.onChange(value)
                }}/>

        {invalid && <Label basic color="red" pointing>{error}</Label>}
    </Form.Field>;
};

const CDropdown = props => {
    return <SUComponent widget={SUDropdown} {...props}/>
};

const CCheckbox = ({input: {value, onChange}, meta: {error, touched}, label, required, ...custom}) => {
    const invalid = error && touched;
    return <Form.Field error={invalid} required={required}>
        <SUCheckbox id={custom.name}
                    {...custom}
                    defaultChecked={value}
                    label={label}
                    onChange={(ev, {checked}) => onChange(checked)}/>
        {invalid && <Label basic color="red" pointing>{error}</Label>}
    </Form.Field>;
};

export const Field = (props) => <RFField component={CInput} {...props}/>;

export const Dropdown = (props) => <RFField component={CDropdown} {...props}/>;

export const FileInput = (props) => <RFField component={CInput} {...props} type="file"/>;

export const Checkbox = (props) => <RFField component={CCheckbox} {...props}/>;
