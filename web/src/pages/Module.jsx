import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import Highlight from 'react-highlight';

import Card from '../components/Card';
import Popup from '../components/Popup';

import { get, set } from '../lib/api';

const Module = ({ match }) => {
	const [config, setConfig] = useState({});
	const [defaultConfig, setDefault] = useState({});
	const [request, setRequest] = useState({ error: null, loading: false, shouldConfirm: false });
	const [command, setCommand] = useState('');
	const [commandRequest, setCommandRequest] = useState({ error: null, loading: false });
	const [commandResponse, setResponse] = useState(null);

	const init = async () => {
		const response = await get('moduleConfig', null, { path: match.params.path });
		setConfig(response.config);
		setDefault(response.default_config);
		setResponse(null);
		setCommand('');
	};

	useEffect(() => {
		init();
	}, [match]);

	const updateEntry = (name) => (e) => {
		setConfig({
			...config,
			[name]: e.target.value
		});
	};

	const saveConfig = async () => {
		setRequest({ ...request, loading: true });

		const response = await set('setModuleConfig', {
			path: match.params.path,
			config: JSON.stringify(config)
		});

		if (!response.error) {
			setRequest({ error: null, loading: false, shouldConfirm: false });
			init();
		} else {
			setRequest({ ...config, error: response.error, loading: false });
		}
	};

	const sendCommand = async (e) => {
		e.preventDefault();

		if (commandRequest.loading) {
			return;
		}

		setCommandRequest(Object.assign({}, commandRequest, { loading: true }));
		setResponse(null);

		const response = await get('ModuleResponse', null, {
			path: match.params.path,
			request: command
		});

		let obj = {};

		try {
			obj = JSON.parse(response.response);
			delete obj.success;
		} catch (e) {
			obj = { response: response.response };
		}

		setCommandRequest({ loading: false, error: false });
		setResponse(JSON.stringify(obj, null, 2));
	};

	const { error, loading, shouldConfirm } = request;

	return (
		<section>
			<article>
				<h1>
					Module configuration
					<nav>
						<button
							className="button success"
							onClick={() => setRequest({ ...request, shouldConfirm: true })}
							type="button"
						>
							Save
						</button>
						<button className="button warning" onClick={() => setConfig(defaultConfig)} type="button">
							Reset
						</button>
					</nav>
				</h1>
				{shouldConfirm && (
					<Popup
						onConfirm={saveConfig}
						type="success"
						cta="Save"
						loading={loading}
						onClose={() => setRequest({ shouldConfirm: null, error: null, loading: false })}
					>
						Save changes to configuration?
						{error && <small className="error">{error}</small>}
					</Popup>
				)}
				<div>
					{config && (
						<Card title={match.params.path} columns>
							{Object.entries(config).map(([title, value]) => (
								<label htmlFor={title} key={title}>
									{title} <input id={title} type="text" onChange={updateEntry(title)} value={value} />
								</label>
							))}
						</Card>
					)}
					<Card title="Send a command" columns>
						<form onSubmit={sendCommand}>
							<label htmlFor="send-command" className="inline">
								<div>
									Command
									<input id="send-command" type="text" onChange={(e) => setCommand(e.target.value)} value={command} />
								</div>
								<button className={`button success${commandRequest.loading ? ' loading' : ''}`} type="submit">
									Send
								</button>
							</label>
							{commandResponse && <Highlight language="json">{commandResponse}</Highlight>}
						</form>
					</Card>
				</div>
			</article>
		</section>
	);
};

Module.propTypes = {
	match: PropTypes.object.isRequired
};

export default Module;
