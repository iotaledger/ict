import React, { Component } from 'react';
import PropTypes from 'prop-types';

import Card from '../components/Card';
import Popup from '../components/Popup';

import { get, set } from '../lib/api';

const defaultState = {
	config: {},
	default_config: {},
	loading: false,
	shouldConfirm: false,
	error: null
};

class Module extends Component {
	static propTypes = {
		match: PropTypes.shape.isRequired
	};

	state = Object.assign({}, defaultState);

	componentDidMount() {
		this.init();
	}

	init = async () => {
		const { match } = this.props;

		const { config, default_config } = await get('moduleConfig', null, { path: match.params.path });

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
		const { match } = this.props;

		this.setState({
			loading: true
		});

		const { error } = await set('setModuleConfig', {
			path: match.params.path,
			config: JSON.stringify(config)
		});

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
		const { match } = this.props;
		const { config, default_config, loading, shouldConfirm, error } = this.state;

		return (
			<section>
				<article>
					<h1>
						Module configuration
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
					<form>
						<Card title={match.params.path} columns>
							{Object.entries(config).map(([title, value]) => (
								<label htmlFor={title} key={title}>
									{title} <input id={title} type="text" onChange={this.updateEntry(title)} value={value} />
								</label>
							))}
						</Card>
					</form>
				</article>
			</section>
		);
	}
}

export default Module;
