import React, { Component } from 'react';

import Card from '../components/Card';
import Popup from '../components/Popup';

import { get, set } from '../lib/api';
import configLabels from '../lib/configLabels';

const defaultState = {
	config: {},
	default_config: {},
	loading: false,
	shouldConfirm: false,
	error: null
};

class Configuration extends Component {
	state = Object.assign({}, defaultState);

	componentDidMount() {
		this.init();
	}

	init = async () => {
		const config = await get('config');
		delete config.success;

		const { default_config } = await get('info');

		this.setState(Object.assign({}, defaultState, { config, default_config }));
	};

	updateEntry = (name) => (e) => {
		const { config } = this.state;

		this.setState({
			config: {
				...config,
				[name]: e.target.value
			}
		});
	};

	saveConfig = async () => {
		const { config } = this.state;

		this.setState({
			loading: true
		});

		const { error } = await set('setConfig', { config: JSON.stringify(config) });

		if (!error) {
			this.init();
		} else {
			this.setState({
				error,
				loading: false
			});
		}
	};

	render() {
		const { config, default_config, loading, shouldConfirm, error } = this.state;

		return (
			<section>
				<article>
					<h1>
						Configuration
						<nav>
							<button className="button success" onClick={() => this.setState({ shouldConfirm: true })} type="button">
								Save
							</button>
							<button
								className="button warning"
								onClick={() => this.setState({ config: Object.assign({}, default_config) })}
								type="button"
							>
								Reset
							</button>
						</nav>
					</h1>
					{shouldConfirm && (
						<Popup
							onConfirm={this.saveConfig}
							type="success"
							cta="Save"
							loading={loading}
							onClose={() => this.setState({ shouldConfirm: null, error: null })}
						>
							Save changes to configuration?
							{error && <small className="error">{error}</small>}
						</Popup>
					)}
					<form className="columns">
						{Object.entries(configLabels).map(([title, items]) => (
							<Card title={title} key={title}>
								{items.map(
									({ name, label }) =>
										config[name] && (
											<label key={name} htmlFor={name}>
												{label} <input id={name} type="text" onChange={this.updateEntry(name)} value={config[name]} />
											</label>
										)
								)}
							</Card>
						))}
					</form>
				</article>
			</section>
		);
	}
}

export default Configuration;
